package org.tasks

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager as AndroidShortcutManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.tasks.ShortcutManager.ShortcutResult

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShortcutManagerTest.RecordingShortcutManager::class])
class ShortcutManagerTest {
    private lateinit var context: Context
    private lateinit var shadow: RecordingShortcutManager
    private lateinit var shortcut: ShortcutInfoCompat

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadow = Shadow.extract(context.getSystemService(AndroidShortcutManager::class.java))
        shortcut = ShortcutInfoCompat.Builder(context, "configured-list")
            .setShortLabel("Configured list")
            .setIntent(Intent(Intent.ACTION_VIEW))
            .build()
    }

    @Test
    fun inAppRequestPinsOnAWorkerThread() = runBlocking {
        val callerThread = Thread.currentThread()

        val result = ShortcutManager(context).createShortcut(shortcut, launcherRequest = false)

        assertEquals(ShortcutResult.Created(null), result)
        assertEquals("configured-list", shadow.pinnedId)
        assertNotSame(callerThread, shadow.pinThread)
    }

    @Test
    fun launcherRequestReturnsResultIntentWithoutPinning() = runBlocking {
        val callerThread = Thread.currentThread()

        val result = ShortcutManager(context).createShortcut(shortcut, launcherRequest = true)

        assertNotNull((result as ShortcutResult.Created).resultIntent)
        assertEquals("configured-list", shadow.resultIntentId)
        assertNotSame(callerThread, shadow.resultIntentThread)
        assertNull(shadow.pinnedId)
    }

    @Test
    fun launcherRefusingPinIsAFailure() = runBlocking {
        shadow.pinAccepted = false

        assertEquals(
            ShortcutResult.Failed,
            ShortcutManager(context).createShortcut(shortcut, launcherRequest = false),
        )
    }

    @Test
    fun pinThrowingIllegalStateIsAFailure() = runBlocking {
        shadow.pinError = IllegalStateException("Calling application must have a foreground activity")

        assertEquals(
            ShortcutResult.Failed,
            ShortcutManager(context).createShortcut(shortcut, launcherRequest = false),
        )
    }

    @Test
    fun pinThrowingIllegalArgumentIsAFailure() = runBlocking {
        shadow.pinError = IllegalArgumentException("Shortcut rejected")

        assertEquals(
            ShortcutResult.Failed,
            ShortcutManager(context).createShortcut(shortcut, launcherRequest = false),
        )
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsNotAFailure() {
        shadow.pinError = CancellationException("cancelled")

        runBlocking {
            ShortcutManager(context).createShortcut(shortcut, launcherRequest = false)
        }
    }

    @Implements(AndroidShortcutManager::class)
    class RecordingShortcutManager {
        var pinAccepted = true
        var pinError: Exception? = null
        var pinnedId: String? = null
        var pinThread: Thread? = null
        var resultIntentId: String? = null
        var resultIntentThread: Thread? = null

        @Implementation
        @Suppress("UNUSED_PARAMETER")
        fun requestPinShortcut(shortcut: ShortcutInfo, resultIntent: IntentSender?): Boolean {
            pinError?.let { throw it }
            pinnedId = shortcut.id
            pinThread = Thread.currentThread()
            return pinAccepted
        }

        @Implementation
        fun createShortcutResultIntent(shortcut: ShortcutInfo): Intent {
            resultIntentId = shortcut.id
            resultIntentThread = Thread.currentThread()
            return Intent()
        }
    }
}
