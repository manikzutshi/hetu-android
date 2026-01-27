package com.aurafarmers.hetu.data.repository

import com.aurafarmers.hetu.data.local.dao.ActionDao
import com.aurafarmers.hetu.data.local.dao.OutcomeDao
import com.aurafarmers.hetu.data.local.entity.ActionEntity
import com.aurafarmers.hetu.data.local.entity.OutcomeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(
    private val actionDao: ActionDao,
    private val outcomeDao: OutcomeDao
) {
    // Actions
    fun getAllActions(): Flow<List<ActionEntity>> = actionDao.getAllActions()
    
    suspend fun insertAction(action: ActionEntity) = actionDao.insert(action)
    
    suspend fun deleteAction(action: ActionEntity) = actionDao.delete(action)
    
    // Outcomes
    fun getAllOutcomes(): Flow<List<OutcomeEntity>> = outcomeDao.getAllOutcomes()
    
    suspend fun insertOutcome(outcome: OutcomeEntity) = outcomeDao.insert(outcome)
    
    suspend fun deleteOutcome(outcome: OutcomeEntity) = outcomeDao.delete(outcome)
    
    // Joint queries could go here if needed, or handled in ViewModel
}
