package com.androidforge.studio.domain.usecase

import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.GitRepository
import com.androidforge.studio.domain.repository.ProjectRepository
import javax.inject.Inject

/** Convenience wrapper so UI code never talks to GitRepository directly for status. */
class GetGitStatusUseCase @Inject constructor(
    private val git: GitRepository,
    private val projects: ProjectRepository,
) {
    suspend operator fun invoke(projectId: Long) =
        projects.getProject(projectId)?.let { git.status(it) }
}
