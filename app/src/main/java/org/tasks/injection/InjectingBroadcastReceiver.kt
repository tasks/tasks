package org.tasks.injection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// https://github.com/google/dagger/issues/1918
abstract class InjectingBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {}
}
