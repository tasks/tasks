package org.tasks.injection

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

abstract class InjectingAppWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        inject(
                (context.applicationContext as InjectingApplication)
                        .component
                        .plus(BroadcastModule()))
        super.onReceive(context, intent)
    }

    protected abstract fun inject(component: BroadcastComponent)
}