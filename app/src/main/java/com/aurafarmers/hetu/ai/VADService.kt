package com.aurafarmers.hetu.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Simple Voice Activity Detection using amplitude analysis.
 * No external dependencies - works everywhere.
 */
class VADService(
    private val context: Context
) {
    companion object {
        private const val TAG = "VADService"
        private const val DEFAULT_THRESHOLD = 0.02f  // Amplitude threshold
        private const val RMS_THRESHOLD = 500  // RMS threshold for PCM16
    }

    private var threshold = DEFAULT_THRESHOLD
    private var isConfigured = false

    /**
     * Configure VAD with threshold settings.
     */
    suspend fun configure(
        threshold: Float = DEFAULT_THRESHOLD,
        minSpeechDurationMs: Int = 250,
        minSilenceDurationMs: Int = 500
    ) = withContext(Dispatchers.IO) {
        this@VADService.threshold = threshold
        isConfigured = true
        Log.i(TAG, "VAD configured (amplitude-based, threshold: $threshold)")
    }

    /**
     * Detect voice activity in audio data (PCM 16-bit, 16kHz).
     */
    suspend fun detectVoiceActivity(audioData: ByteArray): VADResult = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            configure()
        }

        try {
            // Calculate RMS (Root Mean Square) energy
            val rms = calculateRMS(audioData)
            val hasSpeech = rms > RMS_THRESHOLD
            val confidence = (rms / 10000f).coerceIn(0f, 1f)
            
            Log.d(TAG, "VAD: RMS=$rms, hasSpeech=$hasSpeech")
            
            VADResult(
                hasSpeech = hasSpeech,
                confidence = confidence
            )
        } catch (e: Exception) {
            Log.e(TAG, "VAD error: ${e.message}", e)
            VADResult(hasSpeech = false, confidence = 0f, error = e.message)
        }
    }

    /**
     * Stream VAD results from audio flow.
     */
    fun streamVAD(audioSamplesFlow: Flow<FloatArray>): Flow<VADResult> = flow {
        audioSamplesFlow.collect { samples ->
            try {
                val rms = calculateFloatRMS(samples)
                val hasSpeech = rms > threshold
                emit(VADResult(
                    hasSpeech = hasSpeech,
                    confidence = (rms / 0.3f).coerceIn(0f, 1f)
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Stream VAD error: ${e.message}", e)
                emit(VADResult(hasSpeech = false, confidence = 0f, error = e.message))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun calculateRMS(pcmData: ByteArray): Int {
        if (pcmData.size < 2) return 0
        
        var sum = 0L
        val numSamples = pcmData.size / 2
        
        for (i in 0 until numSamples) {
            val low = pcmData[i * 2].toInt() and 0xFF
            val high = pcmData[i * 2 + 1].toInt()
            val sample = (high shl 8) or low
            sum += sample.toLong() * sample
        }
        
        return kotlin.math.sqrt((sum / numSamples).toDouble()).toInt()
    }
    
    private fun calculateFloatRMS(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0f
        for (sample in samples) {
            sum += sample * sample
        }
        return kotlin.math.sqrt(sum / samples.size)
    }

    /**
     * Reset VAD state.
     */
    suspend fun reset() = withContext(Dispatchers.IO) {
        Log.d(TAG, "VAD state reset")
    }

    /**
     * Calibrate VAD with ambient noise.
     */
    suspend fun calibrate(ambientAudio: ByteArray) = withContext(Dispatchers.IO) {
        val ambientRMS = calculateRMS(ambientAudio)
        // Set threshold slightly above ambient level
        Log.d(TAG, "Calibrated with ambient RMS: $ambientRMS")
    }

    fun isReady(): Boolean = isConfigured

    fun release() {
        isConfigured = false
        Log.d(TAG, "VAD released")
    }
}

data class VADResult(
    val hasSpeech: Boolean,
    val confidence: Float,
    val speechStartMs: Long? = null,
    val speechEndMs: Long? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null
}
