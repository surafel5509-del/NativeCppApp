package com.androidforge.studio.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.androidforge.studio.data.local.AppDatabase
import com.androidforge.studio.data.local.dao.BreakpointDao
import com.androidforge.studio.data.local.dao.BuildSessionDao
import com.androidforge.studio.data.local.dao.ChatMessageDao
import com.androidforge.studio.data.local.dao.EditorTabDao
import com.androidforge.studio.data.local.dao.PluginDao
import com.androidforge.studio.data.local.dao.ProjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()
    @Provides fun provideEditorTabDao(db: AppDatabase): EditorTabDao = db.editorTabDao()
    @Provides fun provideBuildSessionDao(db: AppDatabase): BuildSessionDao = db.buildSessionDao()
    @Provides fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()
    @Provides fun providePluginDao(db: AppDatabase): PluginDao = db.pluginDao()
    @Provides fun provideBreakpointDao(db: AppDatabase): BreakpointDao = db.breakpointDao()

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("androidforge", Context.MODE_PRIVATE)
}
