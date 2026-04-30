package com.example.appmind.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "is_monitored")
    val isMonitored: Boolean = false,

    @ColumnInfo(name = "custom_question")
    val customQuestion: String? = null
)
