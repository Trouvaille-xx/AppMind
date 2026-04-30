package com.example.appmind.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_logs")
data class AppLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "question")
    val question: String,

    @ColumnInfo(name = "answer")
    val answer: String,

    @ColumnInfo(name = "action")
    val action: String, // "confirmed" or "cancelled"

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
