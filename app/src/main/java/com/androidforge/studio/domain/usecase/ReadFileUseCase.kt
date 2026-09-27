package com.androidforge.studio.domain.usecase

import com.androidforge.studio.domain.model.FileContent
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.ProjectRepository
import javax.inject.Inject

/** Reads a file with a hard size cap so huge files never freeze the editor. */
class ReadFileUseCase @Inject constructor(
    private val projects: ProjectRepository,
) {
    companion object {
        const val MAX_BYTES = 512L * 1024
    }

    suspend operator fun invoke(project: Project, relativePath: String): FileContent =
        projects.readFile(project, relativePath, MAX_BYTES)
}
