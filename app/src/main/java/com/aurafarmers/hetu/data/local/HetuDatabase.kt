package com.aurafarmers.hetu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aurafarmers.hetu.data.local.dao.ActionDao
import com.aurafarmers.hetu.data.local.dao.InsightDao
import com.aurafarmers.hetu.data.local.dao.MessageDao
import com.aurafarmers.hetu.data.local.dao.OutcomeDao
import com.aurafarmers.hetu.data.local.entity.ActionEntity
import com.aurafarmers.hetu.data.local.entity.InsightEntity
import com.aurafarmers.hetu.data.local.entity.MessageEntity
import com.aurafarmers.hetu.data.local.entity.OutcomeEntity
import com.aurafarmers.hetu.data.local.entity.FeedPostEntity
import com.aurafarmers.hetu.data.local.dao.FeedPostDao
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        ActionEntity::class,
        OutcomeEntity::class,
        MessageEntity::class,
        InsightEntity::class,
        FeedPostEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HetuDatabase : RoomDatabase() {
    
    abstract fun actionDao(): ActionDao
    abstract fun outcomeDao(): OutcomeDao
    abstract fun messageDao(): MessageDao
    abstract fun insightDao(): InsightDao
    abstract fun feedPostDao(): FeedPostDao
    
    companion object {
        private const val DATABASE_NAME = "hetu_database.db"
        
        @Volatile
        private var INSTANCE: HetuDatabase? = null
        
        fun getInstance(context: Context): HetuDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): HetuDatabase {
            // Generate encryption key from device-specific info
            // In production, use Android Keystore for better security
            val passphrase = generatePassphrase(context)
            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
            
            return Room.databaseBuilder(
                context.applicationContext,
                HetuDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory) // SQLCipher encryption
                .fallbackToDestructiveMigration()
                .build()
        }
        
        private fun generatePassphrase(context: Context): String {
            // Combine device-specific values for a unique passphrase
            // Note: In production, use Android Keystore to store this securely
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "hetu_default"
            
            return "hetu_${androidId}_encrypted"
        }
    }
}
