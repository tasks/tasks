package org.tasks

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager as AndroidShortcutManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShortcutManagerTest.RecordingShortcutManager::class])
class ShortcutManagerTest {
    @Test
    fun pinShortcutRequestsTheLauncherOnAWorkerThread() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shortcut = ShortcutInfoCompat.Builder(context, "configured-list")
            .setShortLabel("Configured list")
            .setIntent(Intent(Intent.ACTION_VIEW))
            .build()
        val callerThread = Thread.currentThread()

        assertTrue(ShortcutManager(context).requestPinShortcut(shortcut))

        val manager = context.getSystemService(AndroidShortcutManager::class.java)
        val shadow = Shadow.extract<RecordingShortcutManager>(manager)
        assertEquals("configured-list", shadow.requestedId)
        assertNotSame("The platform shortcut request must run on a worker thread", callerThread, shadow.requestThread)
    }

    @Implements(AndroidShortcutManager::class)
    class RecordingShortcutManager {
        var requestedId: String? = null
        var requestThread: Thread? = null

        @Implementation
        @Suppress("UNUSED_PARAMETER")
        fun requestPinShortcut(shortcut: ShortcutInfo, resultIntent: IntentSender?): Boolean {
            requestedId = shortcut.id
            requestThread = Thread.currentThread()
            return true
        }
    }
}
