package org.tasks.widget

import android.service.quicksettings.TileService
import org.tasks.intents.TaskIntents

class TileService : TileService() {
    override fun onClick() {
        val newTaskIntent = TaskIntents.getNewTaskIntent(this, null)
                .addFlags(TaskIntents.FLAGS)
        startActivityAndCollapse(newTaskIntent)
    }
}