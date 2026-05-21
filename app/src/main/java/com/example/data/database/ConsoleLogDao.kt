package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsoleLogDao {
    @Query("SELECT * FROM console_logs ORDER BY timestamp ASC")
    fun getAllLogs(): Flow<List<ConsoleLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConsoleLog)

    @Query("DELETE FROM console_logs")
    suspend fun clearAllLogs()

    @Query("SELECT count(id) FROM console_logs")
    suspend fun getLogCount(): Int
}
