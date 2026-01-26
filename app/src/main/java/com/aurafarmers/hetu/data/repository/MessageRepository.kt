package com.aurafarmers.hetu.data.repository

import com.aurafarmers.hetu.data.local.dao.MessageDao
import com.aurafarmers.hetu.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {
    fun getAllMessages(): Flow<List<MessageEntity>> = messageDao.getAllMessages()
    
    fun getRecentMessages(limit: Int = 50): Flow<List<MessageEntity>> = messageDao.getRecentMessages(limit)
    
    suspend fun getMessageById(id: Long): MessageEntity? = messageDao.getMessageById(id)
    
    suspend fun insert(message: MessageEntity): Long = messageDao.insert(message)
    
    suspend fun delete(message: MessageEntity) = messageDao.delete(message)
    
    suspend fun deleteAll() = messageDao.deleteAll()
    
    suspend fun getCount(): Int = messageDao.getCount()
}
