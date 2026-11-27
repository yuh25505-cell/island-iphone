package com.island.phone.service

import android.app.Activity
import android.app.Application
import android.os.Bundle

class AppForeground(application: Application) : Application.ActivityLifecycleCallbacks {

    @Volatile
    private var startedCount = 0

    val isForeground: Boolean get() = startedCount > 0

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedCount++
    }

    override fun onActivityStopped(activity: Activity) {
        if (startedCount > 0) startedCount--
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
