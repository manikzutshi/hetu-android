package com.aurafarmers.hetu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents how the user felt at a given time.
 * Examples: "Feeling more energetic", "Sleep was better"
 */
@Entity(tableName = "outcomes")
data class OutcomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val description: String,
    val category: String,
    val date: String, // ISO format: YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    
    // Rating: -2 to +2 (much worse, worse, same, better, much better)
    val rating: Int? = null
)
