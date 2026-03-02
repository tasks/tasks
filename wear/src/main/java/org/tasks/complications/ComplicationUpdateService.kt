package org.tasks.complications

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.tasks.presentation.RefreshTrigger

fun Context.requestComplicationUpdates() {
    val services = listOf(
        TaskCountComplicationService::class.java,
        NextTaskComplicationService::class.java,
        TaskProgressComplicationService::class.java,
    )
    for (service in services) {
        ComplicationDataSourceUpdateRequester
            .create(this, ComponentName(this, service))
            .requestUpdateAll()
    }
}

class ComplicationUpdateService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH_REFRESH) {
            RefreshTrigger.trigger()
            requestComplicationUpdates()
        }
    }

    companion object {
        private const val PATH_REFRESH = "/tasks/refresh"
    }
}
