package com.videodownloader.app.util

import android.content.Context
import androidx.room.*
import com.videodownloader.app.model.DownloadHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadHistoryDao {
    @Query("SELECT * FROM download_history ORDER BY downloadedAt DESC")
    fun getAllHistory(): Flow<List<DownloadHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: DownloadHistory)

    @Delete
    suspend fun delete(history: DownloadHistory)

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}

@Database(entities = [DownloadHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): DownloadHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "video_downloader_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
