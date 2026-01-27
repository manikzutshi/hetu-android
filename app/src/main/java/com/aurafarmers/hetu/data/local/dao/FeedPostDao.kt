package com.aurafarmers.hetu.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aurafarmers.hetu.data.local.entity.FeedPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedPostDao {
    @Query("SELECT * FROM feed_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<FeedPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: FeedPostEntity)

    @Delete
    suspend fun delete(post: FeedPostEntity)
}
