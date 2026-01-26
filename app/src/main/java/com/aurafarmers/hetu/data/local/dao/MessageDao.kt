package com.aurafarmers.hetu.data.local.dao

import androidx.room.*
import com.aurafarmers.hetu.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): MessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity): Long
    
    @Delete
    suspend fun delete(message: MessageEntity)
    
    @Query("DELETE FROM messages")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getCount(): Int
}
