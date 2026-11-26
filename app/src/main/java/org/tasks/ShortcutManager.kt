package org.tasks

import android.content.Context
import android.content.pm.ShortcutManager as AndroidShortcutManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val shortcutManager = context.getSystemService(AndroidShortcutManager::class.java)

    fun reportShortcutUsed(shortcutId: String) {
        shortcutManager?.reportShortcutUsed(shortcutId)
    }

    suspend fun requestPinShortcut(shortcut: ShortcutInfoCompat): Boolean =
        withContext(Dispatchers.IO) {
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }

    companion object {
        const val SHORTCUT_NEW_TASK = "static_new_task"
    }
}