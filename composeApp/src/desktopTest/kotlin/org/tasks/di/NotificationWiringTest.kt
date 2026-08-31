package org.tasks.di

import com.todoroo.astrid.alarms.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.tasks.data.db.Database
import org.tasks.extensions.closeQuietly
import org.tasks.notifications.DesktopNotifier
import org.tasks.notifications.NotificationActionHandler
import org.tasks.notifications.NotificationScheduler
import org.tasks.notifications.Notifier
import org.tasks.service.DesktopCleanup
import org.tasks.service.TaskCleanup

class NotificationWiringTest {
    private var koin: Koin? = null
    private var previousDataDir: String? = null

    @Before
    fun setUp() {
        previousDataDir = System.getProperty(DATA_DIR)
        System.setProperty(DATA_DIR, createTempDirectory().absolutePath)
        resetDirectories()
    }

    @After
    fun tearDown() {
        koin?.let { koin ->
            closeQuietly(TAG, "the database") { koin.get<Database>().close() }
            closeQuietly(TAG, "the scope") { koin.get<CoroutineScope>().cancel() }
        }

        try {
            stopKoin()
        } finally {
            koin = null
            previousDataDir
                ?.let { System.setProperty(DATA_DIR, it) }
                ?: System.clearProperty(DATA_DIR)
            resetDirectories()
        }
    }

    @Test
    fun theDataDirectoryFollowsTheProperty() {
        assertEquals(System.getProperty(DATA_DIR), dataDir.absolutePath)
    }

    @Test
    fun resolvesNotificationGraph() {
        val koin = start()

        val notifier = koin.get<DesktopNotifier>()
        assertSame(notifier, koin.get<Notifier>())
        koin.get<NotificationScheduler>()
        koin.get<NotificationActionHandler>()

        assertNotSame(koin.get<AlarmService>(), koin.get<AlarmService>())
    }

    @Test
    fun deletingATaskGoesThroughTheNotifier() {
        val koin = start()

        assertTrue(koin.get<TaskCleanup>() is DesktopCleanup)
    }

    @Test
    fun theSchedulerAndTheNotifierAreSingletons() {
        val koin = start()

        assertSame(koin.get<NotificationScheduler>(), koin.get<NotificationScheduler>())
        assertSame(koin.get<DesktopNotifier>(), koin.get<DesktopNotifier>())
        assertSame(koin.get<NotificationActionHandler>(), koin.get<NotificationActionHandler>())
    }

    private fun start(): Koin =
        startKoin { modules(commonModule, platformModule()) }.koin.also { koin = it }

    private fun createTempDirectory() =
        java.nio.file.Files.createTempDirectory("tasks-di-test").toFile()
            .also { it.deleteOnExit() }

    companion object {
        private const val DATA_DIR = "tasks.dataDir"

        private const val TAG = "NotificationWiringTest"
    }
}
