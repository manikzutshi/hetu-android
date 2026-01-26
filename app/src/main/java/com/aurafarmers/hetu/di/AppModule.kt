package com.aurafarmers.hetu.di

import android.content.Context
import com.aurafarmers.hetu.ai.AudioRecorder
import com.aurafarmers.hetu.ai.HetuAIManager
import com.aurafarmers.hetu.ai.LLMService
import com.aurafarmers.hetu.ai.STTService
import com.aurafarmers.hetu.ai.TTSService
import com.aurafarmers.hetu.ai.VADService
import com.aurafarmers.hetu.data.local.HetuDatabase
import com.aurafarmers.hetu.data.local.dao.ActionDao
import com.aurafarmers.hetu.data.local.dao.InsightDao
import com.aurafarmers.hetu.data.local.dao.MessageDao
import com.aurafarmers.hetu.data.local.dao.OutcomeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // ============ Database ============
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HetuDatabase {
        return HetuDatabase.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideActionDao(database: HetuDatabase): ActionDao {
        return database.actionDao()
    }
    
    @Provides
    @Singleton
    fun provideOutcomeDao(database: HetuDatabase): OutcomeDao {
        return database.outcomeDao()
    }
    
    @Provides
    @Singleton
    fun provideMessageDao(database: HetuDatabase): MessageDao {
        return database.messageDao()
    }
    
    @Provides
    @Singleton
    fun provideInsightDao(database: HetuDatabase): InsightDao {
        return database.insightDao()
    }
    
    // ============ AI Services ============
    
    // AI Services - Manually instantiated in UI for now to avoid Hilt issues
    // @Provides ...
    
    // @Provides
    // fun provideAudioRecorder...
    
    // @Provides
    // fun provideHetuAIManager...
}
