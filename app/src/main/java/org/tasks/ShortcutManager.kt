package org.tasks

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager as AndroidShortcutManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val shortcutManager = context.getSystemService(AndroidShortcutManager::class.java)

    fun reportShortcutUsed(shortcutId: String) {
        shortcutManager?.reportShortcutUsed(shortcutId)
    }

    suspend fun createShortcut(shortcut: ShortcutInfoCompat, launcherRequest: Boolean): ShortcutResult =
        withContext(Dispatchers.IO) {
            try {
                when {
                    launcherRequest ->
                        ShortcutResult.Created(ShortcutManagerCompat.createShortcutResultIntent(context, shortcut))
                    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null) ->
                        ShortcutResult.Created(null)
                    else -> ShortcutResult.Failed
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: RuntimeException) {
                Timber.w(e, "Could not create shortcut")
                ShortcutResult.Failed
            }
        }

    sealed interface ShortcutResult {
        data class Created(val resultIntent: Intent?) : ShortcutResult
        data object Failed : ShortcutResult
    }

    companion object {
        const val SHORTCUT_NEW_TASK = "static_new_task"
    }
}
