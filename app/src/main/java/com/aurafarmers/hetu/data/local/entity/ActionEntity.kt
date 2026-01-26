package com.aurafarmers.hetu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents something the user tried/did.
 * Examples: "Started taking magnesium", "No screens after 9pm"
 */
@Entity(tableName = "actions")
data class ActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val description: String,
    val category: String,
    val date: String, // ISO format: YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    
    // What the user expects to happen
    val expectation: String? = null,
    
    // When to remind user to check in (days from now)
    val checkInDays: Int? = null,
    
    // Has user checked in on this action?
    val checkedIn: Boolean = false
)
