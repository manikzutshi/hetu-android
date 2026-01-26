package com.aurafarmers.hetu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a journal conversation message.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Optional: link to action/outcome mentioned in this message
    val relatedActionId: Long? = null,
    val relatedOutcomeId: Long? = null
)
