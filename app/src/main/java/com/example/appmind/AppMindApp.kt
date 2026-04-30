package com.example.appmind

import android.app.Application
import com.example.appmind.data.AppDatabase

class AppMindApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
    }
}
