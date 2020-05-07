package org.tasks.injection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

abstract class InjectingBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        inject(
                (context.applicationContext as InjectingApplication)
                        .component
                        .plus(BroadcastModule()))
    }

    protected abstract fun inject(component: BroadcastComponent)
}