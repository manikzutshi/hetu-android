package com.aurafarmers.hetu.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.data.local.entity.MessageEntity
import com.aurafarmers.hetu.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    // Load messages from DB, convert to UI state
    val messages: StateFlow<List<MessageEntity>> = messageRepository.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addUserMessage(content: String) {
        viewModelScope.launch {
            messageRepository.insert(
                MessageEntity(
                    text = content,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun addAiMessage(content: String) {
        viewModelScope.launch {
            messageRepository.insert(
                MessageEntity(
                    text = content,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun clearMessages() {
        viewModelScope.launch {
            messageRepository.deleteAll()
        }
    }
}
