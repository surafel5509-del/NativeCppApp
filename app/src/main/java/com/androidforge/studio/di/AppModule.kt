package com.androidforge.studio.di

import android.content.Context
import com.androidforge.studio.data.build.ToolchainManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideToolchain(@ApplicationContext context: Context): ToolchainManager =
        ToolchainManager(context)
}
