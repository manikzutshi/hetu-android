package com.aurafarmers.hetu.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File
// import io.github.ggerganov.whispercpp.WhisperContext // Failed to resolve
// import com.whispercpp.whisper.WhisperContext // Failed to resolve

/**
 * Service for Speech-to-Text.
 * 
 * NOTE: Whisper integration encountered build issues (library class resolution).
 * Reverting to mock until exact package name is confirmed.
 */
// @Singleton
class STTService(
    private val context: Context
) {
    
    private var isModelLoaded = false
    private val MODEL_PATH = "/sdcard/Download/ggml-tiny.en.bin"
    
    /**
     * Load the STT model.
     */
    suspend fun loadModel() = withContext(Dispatchers.IO) {
        val modelFile = File(MODEL_PATH)
        if (modelFile.exists()) {
            println("STT: Model file found, but library integration is pending.")
            isModelLoaded = true
        } else {
            println("STT: Model file not found at $MODEL_PATH")
        }
    }
    
    /**
     * Transcribe audio data to text.
     */
    suspend fun transcribe(audioData: ByteArray): TranscriptionResult = withContext(Dispatchers.IO) {
        if (!isModelLoaded) {
            return@withContext TranscriptionResult(
                text = "[Model not loaded]", 
                confidence = 0f, 
                error = "Model not loaded"
            )
        }
        
        // Mock response for now
        TranscriptionResult(
            text = "[Voice Input Placeholder - Whisper Lib Issue]",
            confidence = 1.0f
        )
    }
    
    /**
     * Simple transcription without detailed result.
     */
    suspend fun transcribeSimple(audioData: ByteArray): String {
        return transcribe(audioData).text
    }
    
    /**
     * Unload the model.
     */
    suspend fun unloadModel() = withContext(Dispatchers.IO) {
        isModelLoaded = false
    }
    
    fun isLoaded(): Boolean = isModelLoaded
}

data class TranscriptionResult(
    val text: String,
    val confidence: Float,
    val detectedLanguage: String? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null && text.isNotBlank()
}
