package com.example.data

import kotlinx.coroutines.flow.Flow

class RecentFileRepository(private val recentFileDao: RecentFileDao) {
    val allRecentFiles: Flow<List<RecentFile>> = recentFileDao.getAllRecentFiles()

    suspend fun insert(recentFile: RecentFile) {
        recentFileDao.deleteRecentFileByUri(recentFile.uriString) // Prevent duplicates
        recentFileDao.insertRecentFile(recentFile)
    }

    suspend fun deleteById(id: Int) {
        recentFileDao.deleteRecentFileById(id)
    }

    suspend fun deleteAll() {
        recentFileDao.deleteAllRecentFiles()
    }
}
