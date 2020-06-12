package org.tasks

import android.app.Application
import android.content.pm.ShortcutManager
import com.todoroo.andlib.utility.AndroidUtilities
import org.tasks.injection.ApplicationScope
import javax.inject.Inject

@ApplicationScope
class ShortcutManager @Inject constructor(context: Application) {
    private val shortcutManager: ShortcutManager? = if (AndroidUtilities.atLeastNougatMR1()) {
        context.getSystemService(ShortcutManager::class.java)
    } else {
        null
    }

    fun reportShortcutUsed(shortcutId: String) {
        if (AndroidUtilities.atLeastNougatMR1()) {
            shortcutManager?.reportShortcutUsed(shortcutId)
        }
    }

    companion object {
        const val SHORTCUT_NEW_TASK = "static_new_task"
    }
}