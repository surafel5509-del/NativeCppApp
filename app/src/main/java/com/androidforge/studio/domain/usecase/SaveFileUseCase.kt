package com.androidforge.studio.domain.usecase

import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.ProjectRepository
import javax.inject.Inject

/** Writes an editor buffer back to disk. */
class SaveFileUseCase @Inject constructor(
    private val projects: ProjectRepository,
) {
    suspend operator fun invoke(project: Project, relativePath: String, content: String): Boolean =
        projects.writeFile(project, relativePath, content)
}
