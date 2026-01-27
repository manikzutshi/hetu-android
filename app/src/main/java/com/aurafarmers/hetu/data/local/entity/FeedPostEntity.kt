package com.aurafarmers.hetu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_posts")
data class FeedPostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaUri: String,
    val caption: String?,
    val location: String?,
    val timestamp: Long,
    val mediaType: String // "image" or "video"
)
