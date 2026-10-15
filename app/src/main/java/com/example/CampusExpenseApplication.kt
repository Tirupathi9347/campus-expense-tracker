package com.example

import android.app.Application
import com.example.data.AppContainer
import com.example.data.DefaultAppContainer

/**
 * Base Application class for Campus Expense Tracker.
 * Initializes and exposes the AppContainer instance for ViewModel factories.
 */
class CampusExpenseApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
