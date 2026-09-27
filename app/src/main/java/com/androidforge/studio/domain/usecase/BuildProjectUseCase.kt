package com.androidforge.studio.domain.usecase

import com.androidforge.studio.domain.model.BuildSession
import com.androidforge.studio.domain.model.BuildTarget
import com.androidforge.studio.domain.repository.BuildRepository
import com.androidforge.studio.domain.repository.ProjectRepository
import javax.inject.Inject

/** One-click build entry point used by the Build screen and WorkManager worker. */
class BuildProjectUseCase @Inject constructor(
    private val builds: BuildRepository,
    private val projects: ProjectRepository,
) {
    suspend operator fun invoke(projectId: Long, target: BuildTarget, cloud: Boolean = false): Result<BuildSession> {
        val project = projects.getProject(projectId)
            ?: return Result.failure(IllegalArgumentException("Project $projectId not found"))
        return try {
            builds.startBuild(project.id, target, cloud)
            Result.success(
                BuildSession(projectId = project.id, target = target)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cancel() = builds.cancelBuild()
}
