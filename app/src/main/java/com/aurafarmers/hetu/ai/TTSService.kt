package com.aurafarmers.hetu.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Service for Text-to-Speech.
 * 
 * NOTE: Uses Android's built-in TTS until RunAnywhere SDK is available.
 */
// @Singleton
class TTSService(
    private val context: Context
) {
    
    private var tts: TextToSpeech? = null
    private var isVoiceLoaded = false
    
    /**
     * Load the TTS voice.
     */
    suspend fun loadVoice() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    isVoiceLoaded = true
                }
                continuation.resume(Unit)
            }
        }
    }
    
    /**
     * Speak text aloud.
     */
    suspend fun speak(
        text: String,
        rate: Float = 1.0f,
        pitch: Float = 1.0f
    ): SpeakResult = withContext(Dispatchers.IO) {
        if (!isVoiceLoaded || tts == null) {
            return@withContext SpeakResult(
                success = false,
                duration = 0.0,
                error = "TTS not initialized"
            )
        }
        
        tts?.setSpeechRate(rate)
        tts?.setPitch(pitch)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hetu_tts")
        
        SpeakResult(
            success = true,
            duration = text.length * 0.05 // Rough estimate
        )
    }
    
    /**
     * Stop any ongoing speech.
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        tts?.stop()
    }
    
    /**
     * Check if currently speaking.
     */
    fun isSpeaking(): Boolean = tts?.isSpeaking == true
    
    /**
     * Unload the voice.
     */
    suspend fun unloadVoice() = withContext(Dispatchers.IO) {
        tts?.shutdown()
        tts = null
        isVoiceLoaded = false
    }
    
    fun isLoaded(): Boolean = isVoiceLoaded
}

data class SpeakResult(
    val success: Boolean,
    val duration: Double,
    val error: String? = null
)

data class SynthesisResult(
    val audioData: ByteArray?,
    val duration: Double,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null && audioData != null
}
