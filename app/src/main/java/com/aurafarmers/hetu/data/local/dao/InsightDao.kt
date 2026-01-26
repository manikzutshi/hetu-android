package com.aurafarmers.hetu.data.local.dao

import androidx.room.*
import com.aurafarmers.hetu.data.local.entity.InsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
    
    @Query("SELECT * FROM insights ORDER BY updatedAt DESC")
    fun getAllInsights(): Flow<List<InsightEntity>>
    
    @Query("SELECT * FROM insights WHERE confidence = :confidence ORDER BY occurrences DESC")
    fun getInsightsByConfidence(confidence: String): Flow<List<InsightEntity>>
    
    @Query("SELECT * FROM insights WHERE id = :id")
    suspend fun getInsightById(id: Long): InsightEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: InsightEntity): Long
    
    @Update
    suspend fun update(insight: InsightEntity)
    
    @Delete
    suspend fun delete(insight: InsightEntity)
    
    @Query("DELETE FROM insights")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM insights")
    suspend fun getCount(): Int
}
