package org.tasks.widget

import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import org.tasks.intents.TaskIntents

class TileService : TileService() {
    override fun onClick() {
        val newTaskIntent = TaskIntents.getNewTaskIntent(this, null, "tile")
                .addFlags(TaskIntents.FLAGS)
        TileServiceCompat.startActivityAndCollapse(
            this,
            PendingIntentActivityWrapper(
                this,
                0,
                newTaskIntent,
                FLAG_UPDATE_CURRENT,
                false
            )
        )
    }
}