package org.tasks

import android.content.Context
import android.content.pm.ShortcutManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutManager @Inject constructor(@ApplicationContext context: Context) {
    private val shortcutManager = context.getSystemService(ShortcutManager::class.java)

    fun reportShortcutUsed(shortcutId: String) {
        shortcutManager?.reportShortcutUsed(shortcutId)
    }

    companion object {
        const val SHORTCUT_NEW_TASK = "static_new_task"
    }
}