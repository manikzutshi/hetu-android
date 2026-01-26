package com.aurafarmers.hetu.ai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Utility for recording audio from the microphone.
 */
// @Singleton
class AudioRecorder(
    private val context: Context
) {
    
    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    
    /**
     * Check if audio recording permission is granted.
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get the minimum buffer size for recording.
     */
    private fun getBufferSize(): Int {
        return AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    }
    
    /**
     * Start recording and return audio data when stopped.
     * @param maxDurationMs Maximum recording duration in milliseconds
     */
    suspend fun recordAudio(maxDurationMs: Long = 30000): ByteArray = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            return@withContext ByteArray(0)
        }
        
        val bufferSize = getBufferSize()
        val outputStream = ByteArrayOutputStream()
        
        try {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            } catch (e: SecurityException) {
                // Permission denied
                return@withContext ByteArray(0)
            } catch (e: Exception) {
                // Audio system error
                return@withContext ByteArray(0)
            }
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext ByteArray(0)
            }
            
            val buffer = ByteArray(bufferSize)
            isRecording = true
            audioRecord?.startRecording()
            
            val startTime = System.currentTimeMillis()
            
            while (isRecording && coroutineContext.isActive) {
                val bytesRead = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (bytesRead > 0) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                
                // Check max duration
                if (System.currentTimeMillis() - startTime > maxDurationMs) {
                    break
                }
            }
            
            outputStream.toByteArray()
        } finally {
            stopRecording()
        }
    }
    
    /**
     * Stream audio samples as a Flow for VAD processing.
     */
    fun streamAudioSamples(): Flow<FloatArray> = flow {
        if (!hasPermission()) {
            return@flow
        }
        
        val bufferSize = getBufferSize()
        
        try {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            } catch (e: SecurityException) {
                // Permission denied
                return@flow
            } catch (e: Exception) {
                // Audio system error  
                return@flow
            }
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return@flow
            }
            
            val buffer = ShortArray(bufferSize / 2)
            isRecording = true
            audioRecord?.startRecording()
            
            while (isRecording && coroutineContext.isActive) {
                val shortsRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (shortsRead > 0) {
                    // Convert shorts to floats (-1.0 to 1.0)
                    val floatBuffer = FloatArray(shortsRead)
                    for (i in 0 until shortsRead) {
                        floatBuffer[i] = buffer[i] / 32768.0f
                    }
                    emit(floatBuffer)
                }
            }
        } finally {
            stopRecording()
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Stop recording.
     */
    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
    }
    
    /**
     * Check if currently recording.
     */
    fun isRecording(): Boolean = isRecording
}
