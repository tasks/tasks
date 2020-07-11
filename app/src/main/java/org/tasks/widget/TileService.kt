package org.tasks.widget

import android.os.Build.VERSION_CODES
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import org.tasks.intents.TaskIntents

@RequiresApi(api = VERSION_CODES.N)
class TileService : TileService() {
    override fun onClick() {
        val newTaskIntent = TaskIntents.getNewTaskIntent(this, null)
                .addFlags(TaskIntents.FLAGS)
        startActivityAndCollapse(newTaskIntent)
    }
}