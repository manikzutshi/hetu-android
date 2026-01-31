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
        // Support any GGUF model - user selects via file picker
        private const val MODEL_FILENAME = "model.gguf"  // Renamed on copy
        // Minimum valid model size (300MB - support smaller quantized models)
        private const val MIN_MODEL_SIZE_BYTES = 300_000_000L
        // System prompt for Hetu - balanced personality
        private const val SYSTEM_PROMPT = """You are Hetu, a warm and insightful wellness companion. You help users reflect on their habits, emotions, and daily experiences. Provide thoughtful, personalized responses based on what they share. Be supportive but also offer gentle insights when you notice patterns."""
    }
    
    private var smolLM: SmolLM? = null
    private var isModelLoaded = false
    private var loadError: String? = null
    
    /**
     * Get the model file in app storage - searches for any .gguf file
     * Defaults to "model.gguf" if none found, for backward compatibility
     */
    private fun getModelFile(): File {
        val appDir = context.getExternalFilesDir(null) ?: context.filesDir
        // 1. Check for any existing .gguf file
        val existingModels = appDir.listFiles { file -> 
            file.isFile && file.name.endsWith(".gguf", ignoreCase = true) 
        }
        
        if (existingModels != null && existingModels.isNotEmpty()) {
            // Return the largest one (likely the main model)
            val best = existingModels.maxByOrNull { it.length() }
            if (best != null) return best
        }
        
        // 2. Default fallback
        return File(appDir, "model.gguf")
    }
    
    /**
     * Get the name of the file from a URI
     */
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) result = cursor.getString(index)
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "Error getting filename", e) }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) result = result?.substring(cut!! + 1)
        }
        return result ?: "model.gguf"
    }

    /**
     * Get the model path - first checks app storage, then scans Downloads for any .gguf file
     */
    private fun getModelPath(): String? {
        // First check app's private storage (uses dynamic lookup)
        val modelFile = getModelFile()
        if (modelFile.exists() && modelFile.canRead() && modelFile.length() >= MIN_MODEL_SIZE_BYTES) {
            Log.d(TAG, "Found valid model in app storage: ${modelFile.name} (${modelFile.length() / 1024 / 1024} MB)")
            return modelFile.absolutePath
        }
        
        // Check if there's a partial/corrupted file
        if (modelFile.exists() && modelFile.length() < MIN_MODEL_SIZE_BYTES) {
            Log.w(TAG, "Found incomplete model file (${modelFile.length() / 1024 / 1024} MB), deleting...")
            modelFile.delete()
        }
        
        // Scan Downloads folder for any .gguf file
        val downloadDirs = listOf(
            File("/sdcard/Download"),
            File("/storage/emulated/0/Download"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
        )
        
        for (dir in downloadDirs) {
            if (dir.exists() && dir.isDirectory) {
                val ggufFiles = dir.listFiles { file -> 
                    file.isFile && file.name.endsWith(".gguf", ignoreCase = true) && file.length() >= MIN_MODEL_SIZE_BYTES
                }
                if (ggufFiles != null && ggufFiles.isNotEmpty()) {
                    // Use the largest .gguf file (likely the best model)
                    val bestModel = ggufFiles.maxByOrNull { it.length() }
                    if (bestModel != null) {
                        Log.d(TAG, "Found model in Downloads: ${bestModel.name} (${bestModel.length() / 1024 / 1024} MB)")
                        return bestModel.absolutePath
                    }
                }
            }
        }
        
        Log.e(TAG, "No valid .gguf model found in app storage or Downloads")
        return null
    }
    
    /**
     * Get the name of the detected model file
     */
    fun getModelName(): String? {
        val modelPath = getModelPath() ?: return null
        return File(modelPath).name
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
        val appDir = context.getExternalFilesDir(null) ?: context.filesDir
        
        // Use original filename from URI
        val originalName = getFileName(uri)
        val destFile = File(appDir, originalName)
        
        try {
            // Cleanup: Delete OTHER .gguf files to save space and avoid confusion
            appDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".gguf", ignoreCase = true) && file.name != originalName) {
                    Log.i(TAG, "Deleting old/other model: ${file.name}")
                    file.delete()
                }
            }
            
            // Delete if existing (to overwrite)
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
                loadError = "Model file not found. Please select a GGUF model file from Settings."
                Log.e(TAG, loadError!!)
                return@withContext false
            }
            
            val modelFile = File(modelPath)
            Log.i(TAG, "Loading model from $modelPath (size: ${modelFile.length() / 1024 / 1024} MB)...")
            
            smolLM = SmolLM()
            smolLM?.load(
                modelPath = modelPath,
                params = SmolLM.InferenceParams(
                    minP = 0.05f,
                    temperature = 0.8f,      // Slightly more creative
                    storeChats = true,
                    contextSize = 4096,      // Increased for Qwen 2.5 3B
                    numThreads = 8,          // Good for Dimensity 7200
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
