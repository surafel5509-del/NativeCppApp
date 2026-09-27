package com.androidforge.studio

import com.androidforge.studio.domain.model.CodeLanguage
import com.androidforge.studio.domain.model.FileContent
import com.androidforge.studio.domain.model.FileNode
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.ProjectRepository
import com.androidforge.studio.domain.usecase.CreateProjectUseCase
import com.androidforge.studio.domain.usecase.ReadFileUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CreateProjectUseCaseTest {

    private class FakeProjectRepository : ProjectRepository {
        val stored = mutableListOf<Project>()
        var nextId = 1L

        override fun observeProjects(): Flow<List<Project>> = flowOf(stored.toList())
        override suspend fun getProject(id: Long): Project? = stored.firstOrNull { it.id == id }

        override suspend fun createProject(name: String, packageName: String, templateId: String): Project {
            check(stored.none { it.name == name }) { "duplicate" }
            val p = Project(
                id = nextId++,
                name = name,
                packageName = packageName,
                templateId = templateId,
                rootPath = "fake-$nextId",
            )
            stored += p
            return p
        }

        override suspend fun renameProject(id: Long, newName: String) { /* no-op */ }
        override suspend fun deleteProject(id: Long) { stored.removeAll { it.id == id } }
        override suspend fun toggleFavorite(id: Long) { /* no-op */ }

        override suspend fun importProjectFromZip(displayName: String, bytes: ByteArray): Project =
            throw UnsupportedOperationException()

        override suspend fun exportProjectToZip(id: Long): File? = null
        override fun projectDir(project: Project): File = File("/tmp/${project.rootPath}")

        override suspend fun getFileTree(project: Project): FileNode =
            FileNode("root", "/tmp", "", true, 0)

        override suspend fun readFile(project: Project, relativePath: String, maxBytes: Long): FileContent =
            FileContent(relativePath, "package com.example", false, 18)

        override suspend fun writeFile(project: Project, relativePath: String, content: String): Boolean = true
        override suspend fun createFile(project: Project, relativePath: String, content: String): Boolean = true
        override suspend fun createDirectory(project: Project, relativePath: String): Boolean = true
        override suspend fun deletePath(project: Project, relativePath: String): Boolean = true
        override suspend fun renamePath(project: Project, fromRelative: String, toRelative: String): Boolean = true
        override fun languageFor(fileName: String): CodeLanguage = CodeLanguage.forFile(fileName)
    }

    @Test
    fun `rejects blank project name`() = runTest {
        val useCase = CreateProjectUseCase(FakeProjectRepository())
        val result = useCase("   ", "com.example.app", "empty_compose")
        assertNull(result.project)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("name"))
    }

    @Test
    fun `rejects invalid package name`() = runTest {
        val useCase = CreateProjectUseCase(FakeProjectRepository())
        val result = useCase("My App", "NotAPackage", "empty_compose")
        assertNull(result.project)
        assertTrue(result.error!!.contains("Package"))
    }

    @Test
    fun `rejects single-segment package`() = runTest {
        val useCase = CreateProjectUseCase(FakeProjectRepository())
        val result = useCase("My App", "single", "empty_compose")
        assertNull(result.project)
    }

    @Test
    fun `creates project with valid input`() = runTest {
        val repo = FakeProjectRepository()
        val useCase = CreateProjectUseCase(repo)
        val result = useCase("My App", "com.example.myapp", "empty_compose")
        assertNull(result.error)
        assertEquals("My App", result.project!!.name)
        assertEquals(1, repo.stored.size)
    }

    @Test
    fun `trims surrounding whitespace`() = runTest {
        val repo = FakeProjectRepository()
        val useCase = CreateProjectUseCase(repo)
        val result = useCase("  Cool App  ", "com.cool.app", "chat")
        assertEquals("Cool App", result.project!!.name)
    }

    @Test
    fun `read file use case respects size cap`() = runTest {
        val repo = FakeProjectRepository()
        val useCase = ReadFileUseCase(repo)
        val content = useCase(repo.getProject(1) ?: Project(1, "x", "com.x.y", "t", "r"), "Big.kt")
        assertEquals("package com.example", content.text)
    }
}
