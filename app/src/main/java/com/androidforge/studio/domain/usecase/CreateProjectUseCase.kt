package com.androidforge.studio.domain.usecase

import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.ProjectRepository
import javax.inject.Inject

/** Creates a project from a template, validating name/package first. */
class CreateProjectUseCase @Inject constructor(
    private val projects: ProjectRepository,
) {
    data class Result(val project: Project?, val error: String?)

    operator suspend fun invoke(name: String, packageName: String, templateId: String): Result {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return Result(null, "Project name is required.")
        if (!PROJECT_NAME.matches(trimmedName)) {
            return Result(null, "Project name may only contain letters, digits, spaces, '_' or '-'.")
        }
        if (!PACKAGE_NAME.matches(packageName.trim())) {
            return Result(null, "Package name must look like com.company.app (lowercase segments).")
        }
        return try {
            val project = projects.createProject(trimmedName, packageName.trim(), templateId)
            Result(project, null)
        } catch (e: Exception) {
            Result(null, e.message ?: "Failed to create project.")
        }
    }

    private companion object {
        val PROJECT_NAME = Regex("^[A-Za-z0-9 _\\-]{1,60}$")
        val PACKAGE_NAME = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*){1,}$")
    }
}
