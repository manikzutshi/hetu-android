package com.aurafarmers.hetu.data.repository

import com.aurafarmers.hetu.data.local.dao.InsightDao
import com.aurafarmers.hetu.data.local.entity.InsightEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightRepository @Inject constructor(
    private val insightDao: InsightDao
) {
    fun getAllInsights(): Flow<List<InsightEntity>> = insightDao.getAllInsights()
    
    fun getInsightsByConfidence(confidence: String): Flow<List<InsightEntity>> = 
        insightDao.getInsightsByConfidence(confidence)
    
    suspend fun getInsightById(id: Long): InsightEntity? = insightDao.getInsightById(id)
    
    suspend fun insert(insight: InsightEntity): Long = insightDao.insert(insight)
    
    suspend fun update(insight: InsightEntity) = insightDao.update(insight)
    
    suspend fun delete(insight: InsightEntity) = insightDao.delete(insight)
    
    suspend fun deleteAll() = insightDao.deleteAll()
    
    suspend fun getCount(): Int = insightDao.getCount()
}
