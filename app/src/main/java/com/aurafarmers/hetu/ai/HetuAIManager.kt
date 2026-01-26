package com.aurafarmers.hetu.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central manager for all AI services.
 * Coordinates initialization and lifecycle of LLM, STT, TTS, and VAD.
 */
// @Singleton
class HetuAIManager(
    private val context: Context,
    val llmService: LLMService,
    val sttService: STTService,
    val ttsService: TTSService,
    val vadService: VADService
) {
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _initializationProgress = MutableStateFlow(0f)
    val initializationProgress: StateFlow<Float> = _initializationProgress.asStateFlow()
    
    private val _initializationStatus = MutableStateFlow("Ready")
    val initializationStatus: StateFlow<String> = _initializationStatus.asStateFlow()
    
    /**
     * Initialize all AI models.
     */
    suspend fun initialize() {
        try {
            _initializationStatus.value = "Loading STT model..."
            _initializationProgress.value = 0.1f
            sttService.loadModel()
            
            _initializationStatus.value = "Loading LLM model..."
            _initializationProgress.value = 0.4f
            llmService.loadModel()
            
            _initializationStatus.value = "Loading TTS voice..."
            _initializationProgress.value = 0.7f
            ttsService.loadVoice()
            
            _initializationStatus.value = "Configuring VAD..."
            _initializationProgress.value = 0.9f
            vadService.configure()
            
            _initializationProgress.value = 1.0f
            _initializationStatus.value = "Ready (Mock Mode)"
            _isInitialized.value = true
            
        } catch (e: Exception) {
            _initializationStatus.value = "Error: ${e.message}"
            _isInitialized.value = false
        }
    }
    
    fun isReady(): Boolean = _isInitialized.value
    
    suspend fun unload() {
        llmService.unloadModel()
        sttService.unloadModel()
        ttsService.unloadVoice()
        _isInitialized.value = false
    }
}
