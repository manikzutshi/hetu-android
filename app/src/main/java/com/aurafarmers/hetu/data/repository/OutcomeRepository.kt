package com.aurafarmers.hetu.data.repository

import com.aurafarmers.hetu.data.local.dao.OutcomeDao
import com.aurafarmers.hetu.data.local.entity.OutcomeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutcomeRepository @Inject constructor(
    private val outcomeDao: OutcomeDao
) {
    fun getAllOutcomes(): Flow<List<OutcomeEntity>> = outcomeDao.getAllOutcomes()
    
    fun getOutcomesByDate(date: String): Flow<List<OutcomeEntity>> = outcomeDao.getOutcomesByDate(date)
    
    fun getOutcomesByCategory(category: String): Flow<List<OutcomeEntity>> = outcomeDao.getOutcomesByCategory(category)
    
    fun getOutcomesInDateRange(startDate: String, endDate: String): Flow<List<OutcomeEntity>> = 
        outcomeDao.getOutcomesInDateRange(startDate, endDate)
    
    suspend fun getOutcomeById(id: Long): OutcomeEntity? = outcomeDao.getOutcomeById(id)
    
    suspend fun insert(outcome: OutcomeEntity): Long = outcomeDao.insert(outcome)
    
    suspend fun update(outcome: OutcomeEntity) = outcomeDao.update(outcome)
    
    suspend fun delete(outcome: OutcomeEntity) = outcomeDao.delete(outcome)
    
    suspend fun deleteAll() = outcomeDao.deleteAll()
    
    suspend fun getCount(): Int = outcomeDao.getCount()
    
    suspend fun getAverageRatingByCategory(category: String): Float? = 
        outcomeDao.getAverageRatingByCategory(category)
}
