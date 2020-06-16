package org.tasks

import android.content.Context
import android.content.pm.ShortcutManager
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutManager @Inject constructor(@ApplicationContext context: Context) {
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