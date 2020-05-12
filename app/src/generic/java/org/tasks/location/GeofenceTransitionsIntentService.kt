package org.tasks.location

import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GeofenceTransitionsIntentService : IntentService("GeofenceTransitionsIntentService") {
    override fun onHandleIntent(intent: Intent?) {}
    class Broadcast : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {}
    }
}