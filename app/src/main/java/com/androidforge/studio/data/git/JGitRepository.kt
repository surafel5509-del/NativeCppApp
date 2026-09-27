package com.androidforge.studio.data.git

import com.androidforge.studio.domain.model.GitCommit
import com.androidforge.studio.domain.model.GitDiff
import com.androidforge.studio.domain.model.GitDiffHunk
import com.androidforge.studio.domain.model.GitStatus
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.GitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** JGit implementation of git status/log/diff/commit/clone/push/pull. */
@Singleton
class JGitRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : GitRepository {

    private fun projectDirOf(project: Project): File = File(context.filesDir, "projects/${project.rootPath}")

    override suspend fun status(project: Project): GitStatus = withContext(Dispatchers.IO) {
        val dir = projectDirOf(project)
        if (!File(dir, ".git").exists()) return@withContext GitStatus(isRepo = false)
        try {
            Git.open(dir).use { git ->
                val st = git.status().call()
                val branch = git.repository.branch ?: "HEAD"
                GitStatus(
                    isRepo = true,
                    branch = branch,
                    clean = st.isClean,
                    staged = (st.added + st.changed + st.removed).distinct(),
                    unstaged = (st.modified + st.missing).distinct(),
                    untracked = st.untracked,
                )
            }
        } catch (e: Exception) {
            GitStatus(isRepo = true, branch = "?", clean = true)
        }
    }

    override suspend fun log(project: Project, limit: Int): List<GitCommit> = withContext(Dispatchers.IO) {
        val dir = projectDirOf(project)
        if (!File(dir, ".git").exists()) return@withContext emptyList()
        try {
            Git.open(dir).use { git ->
                git.log().setMaxCount(limit).call().map { rev ->
                    GitCommit(
                        hash = rev.name,
                        shortHash = rev.name.take(8),
                        author = rev.authorIdent.name,
                        email = rev.authorIdent.emailAddress,
                        timeEpochMs = rev.commitTime * 1000L,
                        message = rev.fullMessage.trim(),
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun diff(project: Project, filePath: String?): List<GitDiff> =
        withContext(Dispatchers.IO) {
            val dir = projectDirOf(project)
            if (!File(dir, ".git").exists()) return@withContext emptyList()
            try {
                Git.open(dir).use { git ->
                    val formatter = DiffFormatter(ByteArrayOutputStream())
                    formatter.setRepository(git.repository)
                    val entries: List<DiffEntry> = if (filePath != null) {
                        formatter.setPathFilter(org.eclipse.jgit.pathfilter.PathFilter.create(filePath))
                        git.diff().call()
                    } else {
                        git.diff().call()
                    }
                    entries.map { entry ->
                        val bos = ByteArrayOutputStream()
                        val df = DiffFormatter(bos)
                        df.setRepository(git.repository)
                        df.format(entry)
                        parseDiff(entry.newPath ?: entry.oldPath, bos.toString())
                    }.filter { it.hunks.isNotEmpty() }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    override suspend fun init(project: Project): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = projectDirOf(project)
            if (File(dir, ".git").exists()) return@withContext true
            Git.init().setDirectory(dir).call().close()
            true
        } catch (e: Exception) { false }
    }

    override suspend fun addAll(project: Project): Boolean = withContext(Dispatchers.IO) {
        try {
            Git.open(projectDirOf(project)).use { git ->
                git.add().addFilepattern(".").call()
                git.status().call() // force refresh
            }
            true
        } catch (e: Exception) { false }
    }

    override suspend fun commit(
        project: Project,
        message: String,
        author: String,
        email: String,
    ): String? = withContext(Dispatchers.IO) {
        try {
            Git.open(projectDirOf(project)).use { git ->
                val ident = PersonIdent(author, email, java.util.Date(), java.util.TimeZone.getDefault())
                val commit = git.commit()
                    .setAuthor(ident)
                    .setCommitter(ident)
                    .setMessage(message)
                    .call()
                commit.name
            }
        } catch (e: Exception) { null }
    }

    override suspend fun checkout(project: Project, ref: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Git.open(projectDirOf(project)).use { git ->
                    git.checkout().setName(ref).call()
                }
                true
            } catch (e: Exception) { false }
        }

    override suspend fun clone(url: String, project: Project, username: String?, token: String?): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val dir = projectDirOf(project)
                val cmd = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(dir)
                if (username != null && token != null) {
                    cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, token))
                }
                cmd.call().close()
                true
            } catch (e: Exception) { false }
        }

    override suspend fun push(project: Project, remote: String, username: String?, token: String?): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Git.open(projectDirOf(project)).use { git ->
                    val cmd = git.push().setRemote(remote)
                    if (username != null && token != null) {
                        cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, token))
                    }
                    cmd.call()
                }
                true
            } catch (e: Exception) { false }
        }

    override suspend fun pull(project: Project, remote: String, username: String?, token: String?): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Git.open(projectDirOf(project)).use { git ->
                    val cmd = git.pull().setRemote(remote)
                    if (username != null && token != null) {
                        cmd.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, token))
                    }
                    cmd.call()
                }
                true
            } catch (e: Exception) { false }
        }

    // ------------------------------------------------------------- util

    private fun parseDiff(path: String, text: String): GitDiff {
        val hunks = mutableListOf<GitDiffHunk>()
        var header = ""
        val lines = mutableListOf<GitDiffHunk.DiffLine>()
        for (line in text.lineSequence()) {
            when {
                line.startsWith("@@") -> {
                    if (header.isNotEmpty()) hunks += GitDiffHunk(header, lines.toList())
                    header = line
                    lines.clear()
                }
                header.isNotEmpty() && (line.startsWith("+") || line.startsWith("-") || line.startsWith(" ")) -> {
                    val type = when {
                        line.startsWith("+") -> GitDiffHunk.DiffLine.Type.ADD
                        line.startsWith("-") -> GitDiffHunk.DiffLine.Type.DEL
                        else -> GitDiffHunk.DiffLine.Type.CONTEXT
                    }
                    lines += GitDiffHunk.DiffLine(type, line)
                }
            }
        }
        if (header.isNotEmpty()) hunks += GitDiffHunk(header, lines.toList())
        return GitDiff(path, hunks)
    }
}
