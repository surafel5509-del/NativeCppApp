package com.androidforge.studio.di

import com.androidforge.studio.data.ai.AiRepositoryImpl
import com.androidforge.studio.data.build.BuildRepositoryImpl
import com.androidforge.studio.data.git.JGitRepository
import com.androidforge.studio.data.plugin.PluginRepositoryImpl
import com.androidforge.studio.data.repository.ProjectRepositoryImpl
import com.androidforge.studio.data.repository.SettingsRepositoryImpl
import com.androidforge.studio.data.terminal.ShellSessionRepository
import com.androidforge.studio.domain.repository.AiRepository
import com.androidforge.studio.domain.repository.BuildRepository
import com.androidforge.studio.domain.repository.GitRepository
import com.androidforge.studio.domain.repository.PluginRepository
import com.androidforge.studio.domain.repository.ProjectRepository
import com.androidforge.studio.domain.repository.SettingsRepository
import com.androidforge.studio.domain.repository.TerminalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds @Singleton
    abstract fun bindBuildRepository(impl: BuildRepositoryImpl): BuildRepository

    @Binds @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindGitRepository(impl: JGitRepository): GitRepository

    @Binds @Singleton
    abstract fun bindPluginRepository(impl: PluginRepositoryImpl): PluginRepository

    @Binds @Singleton
    abstract fun bindTerminalRepository(impl: ShellSessionRepository): TerminalRepository
}
