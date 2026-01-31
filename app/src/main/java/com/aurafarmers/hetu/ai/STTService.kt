package com.aurafarmers.hetu.ai

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject

/**
 * Service for Offline Speech-to-Text using Vosk.
 * Robust model loading (handles nested folders, auto-copying, and system picker).
 */
class STTService(
    private val context: Context
) {
    companion object {
        private const val TAG = "STTService"
        private const val SAMPLE_RATE = 16000f
    }

    private var model: Model? = null
    private var isModelLoaded = false
    private var loadError: String? = null
    private var loadedModelPath: String? = null

    /**
     * Load the Vosk model - scans downloads or uses internal copy
     */
    suspend fun loadModel() = withContext(Dispatchers.IO) {
        try {
            // 1. Check internal storage first
            val internalModelDir = File(context.filesDir, "vosk-model")
            if (isValidVoskModel(internalModelDir)) {
                try {
                    Log.i(TAG, "Loading from internal storage: ${internalModelDir.absolutePath}")
                    model = Model(internalModelDir.absolutePath)
                    loadedModelPath = internalModelDir.absolutePath
                    isModelLoaded = true
                    loadError = null
                    return@withContext
                } catch (e: Exception) {
                    Log.e(TAG, "Internal model corrupt?", e)
                }
            }
            
            // 2. If not found/valid, scan Downloads as fallback
            val downloadPath = findVoskModelInDownloads()
            if (downloadPath != null) {
                // ... same old logic, simplified ...
                loadModelFromPath(downloadPath)
                return@withContext
            }
            
            loadError = "Model not found. Please use 'Select Folder' to locate it."
            isModelLoaded = false
            
        } catch (e: Exception) {
            loadError = "Init failed: ${e.message}"
            isModelLoaded = false
        }
    }
    
    private suspend fun loadModelFromPath(path: String) {
        val internalModelDir = File(context.filesDir, "vosk-model")
        internalModelDir.deleteRecursively()
        internalModelDir.mkdirs()
        File(path).copyRecursively(internalModelDir, overwrite = true)
        model = Model(internalModelDir.absolutePath)
        loadedModelPath = internalModelDir.absolutePath
        isModelLoaded = true
        loadError = null
    }

    /**
     * Copy model from a user-selected Folder URI (DocumentTree)
     */
    suspend fun copyModelFromTreeUri(treeUri: Uri) = withContext(Dispatchers.IO) {
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc == null || !rootDoc.isDirectory) {
                loadError = "Invalid folder selected"
                return@withContext
            }
            
            Log.i(TAG, "Scanning selected folder: ${rootDoc.name}")
            val modelRoot = findVoskRootInTree(rootDoc, 0)
            
            if (modelRoot != null) {
                Log.i(TAG, "Found valid model root: ${modelRoot.name}. Copying...")
                
                val internalModelDir = File(context.filesDir, "vosk-model")
                internalModelDir.deleteRecursively()
                internalModelDir.mkdirs()
                
                copyDocumentFile(modelRoot, internalModelDir)
                
                Log.i(TAG, "Copy complete. Loading...")
                model = Model(internalModelDir.absolutePath)
                loadedModelPath = internalModelDir.absolutePath
                isModelLoaded = true
                loadError = null
            } else {
                loadError = "Selected folder does not contain a valid Vosk model (am/conf missing)"
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Copy from tree failed", e)
            loadError = "Failed to copy: ${e.message}"
            isModelLoaded = false
        }
    }
    
    // Recursive search in DocumentFile tree
    private fun findVoskRootInTree(doc: DocumentFile, depth: Int): DocumentFile? {
        if (depth > 3) return null
        
        if (isValidVoskModelDoc(doc)) return doc
        
        for (file in doc.listFiles()) {
            if (file.isDirectory) {
                val result = findVoskRootInTree(file, depth + 1)
                if (result != null) return result
            }
        }
        return null
    }
    
    private fun isValidVoskModelDoc(doc: DocumentFile): Boolean {
        // Check for child files "am", "conf", or "final.mdl"
        val hasAm = doc.findFile("am")?.exists() == true
        val hasConf = doc.findFile("conf")?.exists() == true
        val hasFinal = doc.findFile("final.mdl")?.exists() == true
        return (hasAm && hasConf) || hasFinal
    }
    
    private fun copyDocumentFile(src: DocumentFile, dest: File) {
        if (src.isDirectory) {
            dest.mkdirs()
            src.listFiles().forEach { file ->
                copyDocumentFile(file, File(dest, file.name ?: "unknown"))
            }
        } else {
            context.contentResolver.openInputStream(src.uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    /**
     * Find Vosk model root directory in Downloads folder (recursive max depth 3)
     */
    private fun findVoskModelInDownloads(): String? {
        val downloadDirs = listOf(
            File("/sdcard/Download"),
            File("/storage/emulated/0/Download"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
        )
        
        for (dir in downloadDirs) {
            if (!dir.exists() || !dir.isDirectory) continue
            val found = findVoskRootRecursive(dir, 0)
            if (found != null) return found.absolutePath
        }
        return null
    }
    
    private fun findVoskRootRecursive(dir: File, depth: Int): File? {
        if (depth > 3) return null
        if (isValidVoskModel(dir)) {
             if (dir.name.contains("vosk", ignoreCase = true) || dir.parentFile?.name?.contains("vosk", ignoreCase = true) == true) {
                return dir
            }
        }
        val files = dir.listFiles() ?: return null
        for (file in files) {
            if (file.isDirectory) {
                if (file.name.contains("vosk", ignoreCase = true) || depth < 2) {
                    val result = findVoskRootRecursive(file, depth + 1)
                    if (result != null) return result
                }
            }
        }
        return null
    }
    
    private fun isValidVoskModel(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val hasAm = File(dir, "am").exists()
        val hasConf = File(dir, "conf").exists()
        val hasFinalMdl = File(dir, "final.mdl").exists()
        return (hasAm && hasConf) || hasFinalMdl
    }

    /**
     * Transcribe audio data (PCM 16-bit, 16kHz).
     */
    suspend fun transcribe(audioData: ByteArray): TranscriptionResult = withContext(Dispatchers.IO) {
        if (!isModelLoaded || model == null) {
            loadModel()
            if (!isModelLoaded) {
                return@withContext TranscriptionResult(
                    text = "",
                    confidence = 0f,
                    error = loadError ?: "Model not loaded"
                )
            }
        }

        try {
            val recognizer = Recognizer(model, SAMPLE_RATE)
            recognizer.acceptWaveForm(audioData, audioData.size)
            val resultJson = recognizer.finalResult
            recognizer.close()
            
            val json = JSONObject(resultJson)
            val text = json.optString("text", "").trim()
            
            Log.d(TAG, "Transcription: '$text'")
            TranscriptionResult(text = text, confidence = 1.0f)
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            if (e.message?.contains("model", ignoreCase = true) == true) {
                isModelLoaded = false
                model = null
            }
            TranscriptionResult(text = "", confidence = 0f, error = e.message)
        }
    }
    
    suspend fun transcribeSimple(audioData: ByteArray): String {
        return transcribe(audioData).text
    }
    
    fun isModelAvailable(): Boolean {
        return File(context.filesDir, "vosk-model").exists() || findVoskModelInDownloads() != null
    }
    
    fun getModelName(): String? {
        // Return name from loaded path or simple name
        return if (isModelLoaded) "Vosk Model (Ready)" else "None"
    }
    
    fun isLoaded(): Boolean = isModelLoaded
    fun getError(): String? = loadError

    suspend fun unloadModel() = withContext(Dispatchers.IO) {
        model?.close()
        model = null
        isModelLoaded = false
        loadedModelPath = null
    }
}

data class TranscriptionResult(
    val text: String,
    val confidence: Float,
    val detectedLanguage: String? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null && text.isNotBlank()
}
