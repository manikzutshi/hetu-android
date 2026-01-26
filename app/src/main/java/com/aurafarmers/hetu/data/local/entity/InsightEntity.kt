package com.aurafarmers.hetu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a detected pattern/insight.
 */
@Entity(tableName = "insights")
data class InsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val title: String,
    val description: String,
    val emoji: String,
    
    // Confidence level: "high", "medium", "low", "needs_data"
    val confidence: String,
    
    // Categories involved in this pattern
    val actionCategory: String?,
    val outcomeCategory: String?,
    
    // How many times this pattern was observed
    val occurrences: Int = 1,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
