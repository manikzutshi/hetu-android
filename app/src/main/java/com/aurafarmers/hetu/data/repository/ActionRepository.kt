package com.aurafarmers.hetu.data.repository

import com.aurafarmers.hetu.data.local.dao.ActionDao
import com.aurafarmers.hetu.data.local.entity.ActionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionRepository @Inject constructor(
    private val actionDao: ActionDao
) {
    fun getAllActions(): Flow<List<ActionEntity>> = actionDao.getAllActions()
    
    fun getActionsByDate(date: String): Flow<List<ActionEntity>> = actionDao.getActionsByDate(date)
    
    fun getActionsByCategory(category: String): Flow<List<ActionEntity>> = actionDao.getActionsByCategory(category)
    
    fun getPendingCheckIns(): Flow<List<ActionEntity>> = actionDao.getPendingCheckIns()
    
    fun getDistinctDates(): Flow<List<String>> = actionDao.getDistinctDates()
    
    suspend fun getActionById(id: Long): ActionEntity? = actionDao.getActionById(id)
    
    suspend fun insert(action: ActionEntity): Long = actionDao.insert(action)
    
    suspend fun update(action: ActionEntity) = actionDao.update(action)
    
    suspend fun delete(action: ActionEntity) = actionDao.delete(action)
    
    suspend fun deleteAll() = actionDao.deleteAll()
    
    suspend fun getCount(): Int = actionDao.getCount()
}
