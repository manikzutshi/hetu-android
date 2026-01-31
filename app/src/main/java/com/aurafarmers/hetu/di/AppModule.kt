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

    @Provides
    @Singleton
    fun provideFeedPostDao(database: HetuDatabase): com.aurafarmers.hetu.data.local.dao.FeedPostDao {
        return database.feedPostDao()
    }
    
    // ============ AI Services ============
    
    @Provides
    @Singleton
    fun provideLLMService(@ApplicationContext context: Context): LLMService {
        return LLMService(context)
    }
    
    @Provides
    @Singleton
    fun provideSTTService(@ApplicationContext context: Context): STTService {
        return STTService(context)
    }
    
    @Provides
    @Singleton
    fun provideVADService(@ApplicationContext context: Context): VADService {
        return VADService(context)
    }
    
    @Provides
    @Singleton
    fun provideTTSService(@ApplicationContext context: Context): TTSService {
        return TTSService(context)
    }
    
    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder {
        return AudioRecorder(context)
    }
    
    @Provides
    @Singleton
    fun provideHetuAIManager(
        @ApplicationContext context: Context,
        llmService: LLMService,
        sttService: STTService,
        ttsService: TTSService,
        vadService: VADService
    ): HetuAIManager {
        return HetuAIManager(context, llmService, sttService, ttsService, vadService)
    }
}
