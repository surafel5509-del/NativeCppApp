package com.androidforge.studio.data.build

import android.content.Context
import com.androidforge.studio.domain.model.BuildStage
import com.androidforge.studio.domain.repository.SettingsRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud build: dispatches a GitHub Actions workflow
 * (`.github/workflows/android-build.yml`) on the configured repository, then
 * polls for the result. Requires a PAT with `repo` + `actions` scopes in
 * Settings ▸ Cloud. Falls back with clear errors when unconfigured.
 */
@Singleton
class CloudBuildClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) {
    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    data class DispatchResult(val success: Boolean, val artifactPath: String?, val error: String?)

    companion object {
        // owner/repo configured by the user via cloudToken format "token|owner/repo|workflow.yml"
        const val API = "https://api.github.com"
    }

    suspend fun dispatchWorkflow(
        projectDir: File,
        displayName: String,
        onLog: suspend (BuildStage, String, Boolean) -> Unit,
    ): DispatchResult = withContext(Dispatchers.IO) {
        val token = settings.cloudToken().first()
        if (token.isBlank()) {
            return@withContext DispatchResult(
                false, null,
                "Cloud build not configured. Add a GitHub token + owner/repo in Settings ▸ Cloud Build."
            )
        }
        val parts = token.split('|', limit = 3)
        if (parts.size < 3) {
            return@withContext DispatchResult(
                false, null,
                "Cloud token format invalid. Use: ghp_xxx|owner/repo|android-build.yml"
            )
        }
        val (pat, repo, workflow) = parts

        try {
            onLog(BuildStage.SYNC, "Dispatching workflow $workflow on $repo…", false)
            val dispatchBody = JsonObject().apply { addProperty("ref", "main") }
            val dispatch = Request.Builder()
                .url("$API/repos/$repo/actions/workflows/$workflow/dispatches")
                .header("Authorization", "Bearer $pat")
                .header("Accept", "application/vnd.github+json")
                .post(gson.toJson(dispatchBody).toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(dispatch).execute().use { resp ->
                if (resp.code == 204) {
                    onLog(BuildStage.SYNC, "Workflow dispatched. Watching latest run…", false)
                } else {
                    val body = resp.body?.string().orEmpty().take(400)
                    return@withContext DispatchResult(false, null, "Dispatch failed (HTTP ${resp.code}): $body")
                }
            }

            // Poll latest workflow runs (max ~10 min)
            repeat(60) {
                kotlinx.coroutines.delay(10_000)
                val runsReq = Request.Builder()
                    .url("$API/repos/$repo/actions/runs?per_page=5")
                    .header("Authorization", "Bearer $pat")
                    .header("Accept", "application/vnd.github+json")
                    .get()
                    .build()
                http.newCall(runsReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val json = gson.fromJson(resp.body?.string(), JsonObject::class.java)
                    val runs = json.getAsJsonArray("workflow_runs") ?: return@use
                    if (runs.size() == 0) return@use
                    val latest = runs[0].asJsonObject
                    val status = latest.get("status")?.asString ?: "queued"
                    val conclusion = latest.get("conclusion")?.let { if (it.isJsonNull) null else it.asString }
                    onLog(BuildStage.SYNC, "Run #${latest.get("run_number")?.asInt}: $status ${conclusion ?: ""}".trim(), false)
                    when {
                        status == "completed" && conclusion == "success" -> {
                            onLog(BuildStage.DONE, "Cloud run succeeded.", false)
                            val artifact = downloadFirstArtifact(pat, repo, latest.get("id")?.asLong ?: -1)
                            return@withContext DispatchResult(true, artifact, null)
                        }
                        status == "completed" -> {
                            return@withContext DispatchResult(false, null, "Cloud run concluded: $conclusion")
                        }
                        else -> {
                            // still running — keep polling
                        }
                    }
                }
            }
            DispatchResult(false, null, "Timed out waiting for the workflow run (10 min). Check the Actions tab.")
        } catch (e: IOException) {
            DispatchResult(false, null, "Network error while talking to GitHub: ${e.message}")
        } catch (e: Exception) {
            DispatchResult(false, null, e.message ?: "Cloud build failed")
        }
    }

    /** Downloads the run's first artifact into filesDir/artifacts/cloud-artifact.zip. */
    private fun downloadFirstArtifact(pat: String, repo: String, runId: Long): String? = try {
        if (runId < 0) null else {
            val listReq = Request.Builder()
                .url("$API/repos/$repo/actions/runs/$runId/artifacts")
                .header("Authorization", "Bearer $pat")
                .header("Accept", "application/vnd.github+json")
                .get()
                .build()
            val archiveUrl: String? = http.newCall(listReq).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val json = gson.fromJson(resp.body?.string(), JsonObject::class.java) ?: return@use null
                val artifacts = json.getAsJsonArray("artifacts") ?: return@use null
                if (artifacts.size() == 0) return@use null
                artifacts[0].asJsonObject.get("archive_download_url")?.asString
            }
            if (archiveUrl == null) {
                null
            } else {
                val dl = Request.Builder()
                    .url(archiveUrl)
                    .header("Authorization", "Bearer $pat")
                    .header("Accept", "application/vnd.github+json")
                    .get()
                    .build()
                http.newCall(dl).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val out = File(context.filesDir, "artifacts/cloud-artifact.zip")
                    out.parentFile?.mkdirs()
                    resp.body?.byteStream()?.use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                    out.absolutePath
                }
            }
        }
    } catch (e: Exception) {
        null
    }
}
