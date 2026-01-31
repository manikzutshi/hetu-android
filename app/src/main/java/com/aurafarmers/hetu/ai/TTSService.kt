package com.aurafarmers.hetu.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Service for Text-to-Speech using Android's built-in TTS engine.
 * Clean, simple, works on all Android devices.
 */
class TTSService(
    private val context: Context
) {
    companion object {
        private const val TAG = "TTSService"
    }

    private var tts: TextToSpeech? = null
    private var isLoaded = false

    /**
     * Initialize the TTS engine.
     */
    suspend fun loadVoice() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    isLoaded = true
                    Log.i(TAG, "TTS initialized successfully")
                } else {
                    Log.e(TAG, "TTS initialization failed with status: $status")
                    isLoaded = false
                }
                continuation.resume(Unit)
            }
        }
    }

    /**
     * Speak the given text.
     */
    suspend fun speak(
        text: String,
        rate: Float = 1.0f,
        pitch: Float = 1.0f
    ): SpeakResult = withContext(Dispatchers.IO) {
        if (!isLoaded || tts == null) {
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
            duration = text.length * 0.05  // Rough estimate
        )
    }

    /**
     * Stop speaking.
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        tts?.stop()
    }

    /**
     * Check if currently speaking.
     */
    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    /**
     * Release TTS resources.
     */
    suspend fun unloadVoice() = withContext(Dispatchers.IO) {
        tts?.shutdown()
        tts = null
        isLoaded = false
    }
    
    fun isLoaded(): Boolean = isLoaded
}

data class SpeakResult(
    val success: Boolean,
    val duration: Double,
    val error: String? = null
)
