package com.aurafarmers.hetu.ai

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import io.shubham0204.smollm.SmolLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Service for on-device LLM inference using SmolLM (llama.cpp).
 * 
 * Provides real, on-device inference using GGUF models.
 */
class LLMService(
    private val context: Context
) {
    companion object {
        private const val TAG = "LLMService"
        // User's actual model filename
        private const val MODEL_FILENAME = "Llama-3.2-3B-Instruct-Q4_K_M.gguf"
        // Minimum valid model size (1.5GB - Q4 3B model should be at least this)
        private const val MIN_MODEL_SIZE_BYTES = 1_500_000_000L
        // System prompt for Hetu's personality
        private const val SYSTEM_PROMPT = """You are Hetu, a kind and empathetic journal companion. 
You are a safe space for people to share their thoughts and feelings.
You listen without judgment. Keep responses concise - 2-3 sentences typically.
Do not give unsolicited advice. Ask thoughtful follow-up questions."""
    }
    
    private var smolLM: SmolLM? = null
    private var isModelLoaded = false
    private var loadError: String? = null
    
    /**
     * Get the model file in app storage
     */
    private fun getModelFile(): File {
        val appDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(appDir, MODEL_FILENAME)
    }
    
    /**
     * Get the model path from app's private storage
     */
    private fun getModelPath(): String? {
        val modelFile = getModelFile()
        if (modelFile.exists() && modelFile.canRead() && modelFile.length() >= MIN_MODEL_SIZE_BYTES) {
            Log.d(TAG, "Found valid model: ${modelFile.absolutePath} (${modelFile.length() / 1024 / 1024} MB)")
            return modelFile.absolutePath
        }
        
        // Check if there's a partial/corrupted file
        if (modelFile.exists() && modelFile.length() < MIN_MODEL_SIZE_BYTES) {
            Log.w(TAG, "Found incomplete model file (${modelFile.length() / 1024 / 1024} MB), deleting...")
            modelFile.delete()
        }
        
        // Try common external paths (may require permission)
        val externalPaths = listOf(
            "/sdcard/Download/$MODEL_FILENAME",
            "/storage/emulated/0/Download/$MODEL_FILENAME",
            "${Environment.getExternalStorageDirectory().absolutePath}/Download/$MODEL_FILENAME"
        )
        
        for (path in externalPaths) {
            val file = File(path)
            if (file.exists() && file.canRead() && file.length() >= MIN_MODEL_SIZE_BYTES) {
                Log.d(TAG, "Found model in external storage: $path (${file.length() / 1024 / 1024} MB)")
                return path
            }
        }
        
        Log.e(TAG, "No valid model found")
        return null
    }
    
    /**
     * Delete any partial/corrupted model file
     */
    fun deletePartialModel(): Boolean {
        val modelFile = getModelFile()
        if (modelFile.exists()) {
            val deleted = modelFile.delete()
            Log.i(TAG, "Deleted partial model: $deleted")
            return deleted
        }
        return true
    }
    
    /**
     * Copy model from a content URI (from file picker) to app storage
     * Uses buffered streaming for large files
     */
    suspend fun copyModelFromUri(uri: Uri, onProgress: (Int) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        val destFile = getModelFile()
        
        try {
            // Delete any existing partial file first
            if (destFile.exists()) {
                destFile.delete()
            }
            
            Log.i(TAG, "Starting model copy to: ${destFile.absolutePath}")
            
            // Get file size for progress
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            val totalSize = fileDescriptor?.statSize ?: 0L
            fileDescriptor?.close()
            
            Log.i(TAG, "Source file size: ${totalSize / 1024 / 1024} MB")
            
            if (totalSize < MIN_MODEL_SIZE_BYTES) {
                Log.e(TAG, "Source file too small (${totalSize / 1024 / 1024} MB), expected at least ${MIN_MODEL_SIZE_BYTES / 1024 / 1024} MB")
                return@withContext false
            }
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(1024 * 1024) // 1MB buffer for large files
                    var bytesRead: Int
                    var totalBytes = 0L
                    var lastProgress = 0
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        
                        // Report progress every ~5%
                        if (totalSize > 0) {
                            val progress = ((totalBytes * 100) / totalSize).toInt()
                            if (progress >= lastProgress + 5) {
                                lastProgress = progress
                                Log.d(TAG, "Copy progress: $progress% (${totalBytes / 1024 / 1024} MB)")
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }
            
            // Verify copy was complete
            val copiedSize = destFile.length()
            Log.i(TAG, "Copy complete: ${copiedSize / 1024 / 1024} MB")
            
            if (copiedSize < MIN_MODEL_SIZE_BYTES) {
                Log.e(TAG, "Copy incomplete! Only ${copiedSize / 1024 / 1024} MB copied")
                destFile.delete()
                return@withContext false
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model: ${e.message}", e)
            // Clean up partial file on error
            if (destFile.exists()) {
                destFile.delete()
            }
            return@withContext false
        }
    }
    
    /**
     * Check if model file exists in accessible location
     */
    fun isModelAvailable(): Boolean = getModelPath() != null
    
    /**
     * Load the LLM model from the device.
     * @return true if model loaded successfully, false otherwise
     */
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelPath = getModelPath()
            if (modelPath == null) {
                loadError = "Model file not found or incomplete. Please select your GGUF model file (~2GB)."
                Log.e(TAG, loadError!!)
                return@withContext false
            }
            
            val modelFile = File(modelPath)
            Log.i(TAG, "Loading model from $modelPath (size: ${modelFile.length() / 1024 / 1024} MB)...")
            
            smolLM = SmolLM()
            smolLM?.load(
                modelPath = modelPath,
                params = SmolLM.InferenceParams(
                    minP = 0.1f,
                    temperature = 0.7f,
                    storeChats = true,
                    contextSize = 2048,
                    numThreads = 4,
                    useMmap = true,
                    useMlock = false
                )
            )
            
            // Add system prompt
            smolLM?.addSystemPrompt(SYSTEM_PROMPT)
            
            isModelLoaded = true
            loadError = null
            Log.i(TAG, "Model loaded successfully!")
            return@withContext true
        } catch (e: AssertionError) {
            loadError = "Failed to read model file. It may be corrupted - please select it again."
            Log.e(TAG, "AssertionError loading model: ${e.message}", e)
            isModelLoaded = false
            return@withContext false
        } catch (e: Exception) {
            loadError = "Error loading model: ${e.message}"
            Log.e(TAG, "Exception loading model: ${e.message}", e)
            isModelLoaded = false
            return@withContext false
        }
    }
    
    /**
     * Simple chat response - gets full response.
     */
    suspend fun chat(userMessage: String): String = withContext(Dispatchers.IO) {
        if (smolLM == null || !isModelLoaded) {
            // Try to load model if not loaded
            if (!loadModel()) {
                return@withContext loadError ?: "Please select a model file first."
            }
        }
        
        return@withContext try {
            Log.d(TAG, "Processing message: $userMessage")
            val response = smolLM?.getResponse(userMessage) ?: "I'm having trouble responding right now."
            Log.d(TAG, "Generated response: $response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response: ${e.message}", e)
            "I encountered an error: ${e.message}"
        }
    }
    
    /**
     * Streaming chat response - emits tokens as they're generated.
     */
    fun chatStream(userMessage: String): Flow<String> = flow {
        if (smolLM == null || !isModelLoaded) {
            emit("Loading model...")
            val loaded = loadModel()
            if (!loaded) {
                emit(loadError ?: "Please select a model file first.")
                return@flow
            }
        }
        
        smolLM?.getResponseAsFlow(userMessage)?.collect { token ->
            emit(token)
        }
    }
    
    /**
     * Unload the model to free memory.
     */
    suspend fun unloadModel() = withContext(Dispatchers.IO) {
        smolLM?.close()
        smolLM = null
        isModelLoaded = false
        Log.i(TAG, "Model unloaded")
    }
    
    /**
     * Check if model is loaded and ready.
     */
    fun isLoaded(): Boolean = isModelLoaded
    
    /**
     * Get the last load error message.
     */
    fun getLoadError(): String? = loadError
    
    /**
     * Get inference speed (tokens per second).
     */
    fun getSpeed(): Float = smolLM?.getResponseGenerationSpeed() ?: 0f
    
    /**
     * Get context length used.
     */
    fun getContextUsed(): Int = smolLM?.getContextLengthUsed() ?: 0
}
