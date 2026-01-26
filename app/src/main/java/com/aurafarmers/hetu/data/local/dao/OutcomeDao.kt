package com.aurafarmers.hetu.data.local.dao

import androidx.room.*
import com.aurafarmers.hetu.data.local.entity.OutcomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutcomeDao {
    
    @Query("SELECT * FROM outcomes ORDER BY timestamp DESC")
    fun getAllOutcomes(): Flow<List<OutcomeEntity>>
    
    @Query("SELECT * FROM outcomes WHERE date = :date ORDER BY timestamp DESC")
    fun getOutcomesByDate(date: String): Flow<List<OutcomeEntity>>
    
    @Query("SELECT * FROM outcomes WHERE category = :category ORDER BY timestamp DESC")
    fun getOutcomesByCategory(category: String): Flow<List<OutcomeEntity>>
    
    @Query("SELECT * FROM outcomes WHERE id = :id")
    suspend fun getOutcomeById(id: Long): OutcomeEntity?
    
    @Query("SELECT * FROM outcomes WHERE date BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getOutcomesInDateRange(startDate: String, endDate: String): Flow<List<OutcomeEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outcome: OutcomeEntity): Long
    
    @Update
    suspend fun update(outcome: OutcomeEntity)
    
    @Delete
    suspend fun delete(outcome: OutcomeEntity)
    
    @Query("DELETE FROM outcomes")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM outcomes")
    suspend fun getCount(): Int
    
    @Query("SELECT AVG(rating) FROM outcomes WHERE category = :category AND rating IS NOT NULL")
    suspend fun getAverageRatingByCategory(category: String): Float?
}
