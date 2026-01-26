package com.aurafarmers.hetu.data.local.dao

import androidx.room.*
import com.aurafarmers.hetu.data.local.entity.ActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionDao {
    
    @Query("SELECT * FROM actions ORDER BY timestamp DESC")
    fun getAllActions(): Flow<List<ActionEntity>>
    
    @Query("SELECT * FROM actions WHERE date = :date ORDER BY timestamp DESC")
    fun getActionsByDate(date: String): Flow<List<ActionEntity>>
    
    @Query("SELECT * FROM actions WHERE category = :category ORDER BY timestamp DESC")
    fun getActionsByCategory(category: String): Flow<List<ActionEntity>>
    
    @Query("SELECT * FROM actions WHERE id = :id")
    suspend fun getActionById(id: Long): ActionEntity?
    
    @Query("SELECT * FROM actions WHERE checkedIn = 0 AND checkInDays IS NOT NULL ORDER BY timestamp ASC")
    fun getPendingCheckIns(): Flow<List<ActionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: ActionEntity): Long
    
    @Update
    suspend fun update(action: ActionEntity)
    
    @Delete
    suspend fun delete(action: ActionEntity)
    
    @Query("DELETE FROM actions")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM actions")
    suspend fun getCount(): Int
    
    @Query("SELECT DISTINCT date FROM actions ORDER BY date DESC")
    fun getDistinctDates(): Flow<List<String>>
}
