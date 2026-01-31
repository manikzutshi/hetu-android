package com.aurafarmers.hetu.ui.screens.journal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.ai.*
import com.aurafarmers.hetu.data.local.entity.MessageEntity
import com.aurafarmers.hetu.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    val vadService: VADService,
    val sttService: STTService,
    val ttsService: TTSService,
    val llmService: LLMService,
    val audioRecorder: AudioRecorder
) : ViewModel() {
    
    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()
    
    private val _currentStatus = MutableStateFlow("Idle")
    val currentStatus = _currentStatus.asStateFlow()

    private var listeningJob: Job? = null

    val messages: StateFlow<List<MessageEntity>> = messageRepository.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startWakeWordLoop() {
        if (listeningJob?.isActive == true) return
        
        listeningJob = viewModelScope.launch {
            _currentStatus.value = "Waiting for 'Hetu'..."
            Log.d("JournalVM", "Starting Wake Word Loop")
            
            while (isActive) {
                // 1. Capture a short audio snippet (e.g., 2 seconds) to check for speech
                // In a real optimized loop, we'd stream. Here we do chunks for simplicity.
                // 1. Capture a short audio snippet
                val audioData = audioRecorder.recordAudio(2000)
                
                if (audioData.isNotEmpty()) {
                    // 2. Check VAD
                    val vadResult = vadService.detectVoiceActivity(audioData)
                    if (vadResult.hasSpeech) {
                        Log.d("JournalVM", "Speech detected! Checking for wake word...")
                        _currentStatus.value = "Listening..."
                        
                        // 3. Transcribe
                        val text = sttService.transcribeSimple(audioData)
                        Log.d("JournalVM", "Transcribed: $text")
                        
                        // 4. Check for Wake Word
                        if (text.contains("Hetu", ignoreCase = true) || text.contains("Hello", ignoreCase = true)) {
                            Log.d("JournalVM", "Wake Word Detected!")
                            activateAssistant()
                        }
                    }
                }
                delay(100)
            }
        }
    }

    private fun activateAssistant() {
        viewModelScope.launch {
            listeningJob?.cancel() // Stop wake loop
            _isListening.value = true
            _currentStatus.value = "I'm listening..."
            
            triggerHapticFeedback() 
            ttsService.speak("Yes?") 
            
            // Start full recording for query
            val queryAudio = audioRecorder.recordAudio(5000)
            _isListening.value = false
            _isProcessing.value = true
            _currentStatus.value = "Thinking..."
            
            val queryText = sttService.transcribeSimple(queryAudio)
            if (queryText.isNotBlank()) {
                addUserMessage(queryText)
                
                // LLM Response
                val response = llmService.chat(queryText)
                addAiMessage(response)
                
                ttsService.speak(response)
            } else {
                _currentStatus.value = "Didn't hear anything."
            }
            
            _isProcessing.value = false
            delay(500)
            // Resume wake loop
            startWakeWordLoop() 
        }
    }


    fun stopListening() {
        listeningJob?.cancel()
        _isListening.value = false
        _currentStatus.value = "Stopped"
    }
    
    // Initializers helpers called from UI - REMOVED (injected via constructor)
    // fun attachServices...

    private fun triggerHapticFeedback() {
        // Implement haptic feedback
    }

    fun addUserMessage(content: String) {
        viewModelScope.launch {
            messageRepository.insert(MessageEntity(text = content, isUser = true, timestamp = System.currentTimeMillis()))
        }
    }

    fun addAiMessage(content: String) {
        viewModelScope.launch {
            messageRepository.insert(MessageEntity(text = content, isUser = false, timestamp = System.currentTimeMillis()))
        }
    }
    
    fun clearMessages() {
        viewModelScope.launch { messageRepository.deleteAll() }
    }
}
