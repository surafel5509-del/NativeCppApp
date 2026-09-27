package com.androidforge.studio.worker

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.androidforge.studio.AndroidForgeApp
import com.androidforge.studio.domain.model.BuildTarget
import com.androidforge.studio.domain.repository.BuildRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs a build in the background via WorkManager so the user can leave the
 * app; progress is surfaced through a notification channel.
 *
 * Enqueue with:
 * ```
 * BuildWorker.enqueue(context, projectId = 1L, target = "APK", cloud = false)
 * ```
 */
@HiltWorker
class BuildWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val builds: BuildRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val projectId = inputData.getLong(KEY_PROJECT_ID, -1L)
        val target = inputData.getString(KEY_TARGET)?.let {
            runCatching { BuildTarget.valueOf(it) }.getOrDefault(BuildTarget.APK)
        } ?: BuildTarget.APK
        val cloud = inputData.getBoolean(KEY_CLOUD, false)

        if (projectId <= 0) return Result.failure(workDataOf(KEY_ERROR to "No project id"))

        return try {
            runCatching { setForeground(getForegroundInfo()) }
            builds.startBuild(projectId, target, cloud)

            // Observe until terminal state (bounded at 15 min)
            val terminal = withTimeoutOrNull(15 * 60 * 1000L) {
                builds.observeBuild().first {
                    it.status.name in listOf("SUCCESS", "FAILED", "CANCELLED")
                }
            }
            when {
                terminal == null -> {
                    builds.cancelBuild()
                    Result.failure(workDataOf(KEY_ERROR to "Timed out"))
                }
                terminal.status.name == "SUCCESS" -> {
                    notify("Build succeeded: ${terminal.artifactPath?.substringAfterLast('/') ?: ""}")
                    Result.success(workDataOf(KEY_ARTIFACT to (terminal.artifactPath ?: "")))
                }
                else -> Result.failure(workDataOf(KEY_ERROR to "Build ${terminal.status}"))
            }
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "error")))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification: Notification = NotificationCompat.Builder(applicationContext, AndroidForgeApp.CHANNEL_BUILDS)
            .setContentTitle("AndroidForge build")
            .setContentText("Running…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun notify(text: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, AndroidForgeApp.CHANNEL_BUILDS)
            .setContentTitle("Build finished")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        nm.notify(FINISHED_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val KEY_PROJECT_ID = "projectId"
        private const val KEY_TARGET = "target"
        private const val KEY_CLOUD = "cloud"
        private const val KEY_ERROR = "error"
        private const val KEY_ARTIFACT = "artifact"
        private const val NOTIFICATION_ID = 4211
        private const val FINISHED_NOTIFICATION_ID = 4212

        fun enqueue(context: Context, projectId: Long, target: BuildTarget, cloud: Boolean) {
            val request = OneTimeWorkRequestBuilder<BuildWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(
                    workDataOf(
                        KEY_PROJECT_ID to projectId,
                        KEY_TARGET to target.name,
                        KEY_CLOUD to cloud,
                    )
                )
                .addTag("androidforge-build")
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
