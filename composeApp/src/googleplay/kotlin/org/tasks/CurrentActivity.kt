package org.tasks

import android.app.Activity
import android.app.Application
import android.os.Bundle

@Deprecated("Delete this hack when Microsoft To Do is retired")
class CurrentActivity : Application.ActivityLifecycleCallbacks {
    @Volatile
    var activity: Activity? = null
        private set

    override fun onActivityStarted(activity: Activity) {
        this.activity = activity
    }

    override fun onActivityStopped(activity: Activity) {
        if (this.activity === activity) {
            this.activity = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
