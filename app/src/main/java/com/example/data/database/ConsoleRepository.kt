package com.example.data.database

import kotlinx.coroutines.flow.Flow

class ConsoleRepository(private val consoleLogDao: ConsoleLogDao) {
    val allLogs: Flow<List<ConsoleLog>> = consoleLogDao.getAllLogs()

    suspend fun log(tag: String, message: String) {
        consoleLogDao.insertLog(ConsoleLog(tag = tag, message = message))
    }

    suspend fun clearLogs() {
        consoleLogDao.clearAllLogs()
    }
}
