package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val uriString: String,
    val size: String,
    val timestamp: Long = System.currentTimeMillis()
)
