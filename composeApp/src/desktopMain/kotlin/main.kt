import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import org.tasks.TasksBuildConfig
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.PostHogReporting
import org.tasks.analytics.Reporting
import org.tasks.App
import org.tasks.auth.TasksServerEnvironment
import org.tasks.compose.StableWindowSize
import org.tasks.jobs.BackgroundWork
import org.tasks.requestForeground
import org.tasks.setQuitting
import org.tasks.PlatformConfiguration
import org.tasks.TaskRequests
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.recordInstallIfNeeded
import org.tasks.viewmodel.PendingTaskSaves
import org.tasks.http.EncryptedCookieStore
import at.bitfire.cert4android.DesktopUserDecisionRegistry
import org.tasks.ssl.TrustCertificateDialog
import org.tasks.sse.SseClient
import org.tasks.sync.SyncSource
import org.tasks.sync.microsoft.DesktopMicrosoftClientProvider
import org.tasks.sync.microsoft.MicrosoftClientProvider
import org.tasks.di.commonModule
import org.tasks.di.dataDir
import org.tasks.di.logDir
import org.tasks.di.platformModule
import org.tasks.logging.FileLogWriter
import org.tasks.logging.logStartup
import java.awt.Desktop
import java.awt.Dimension
import java.awt.desktop.QuitStrategy
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import org.tasks.extensions.openInBrowser
import org.tasks.extensions.step

private const val TAG = "main"

private val portFile = File(dataDir, ".ipc_port")
private val lockFile = File(dataDir, ".lock")
private var lockChannel: FileChannel? = null
private var ipcServer: ServerSocket? = null

private fun acquireLock(): Boolean {
    return try {
        lockFile.parentFile?.mkdirs()
        lockChannel = RandomAccessFile(lockFile, "rw").channel
        lockChannel?.tryLock() != null
    } catch (e: Exception) {
        Logger.e(e) { "Failed to acquire lock" }
        false
    }
}

private fun signalExistingInstance(): Boolean {
    return try {
        val port = portFile.readText().trim().toInt()
        Socket(InetAddress.getLoopbackAddress(), port).use { it.getOutputStream().write(1) }
        true
    } catch (e: Exception) {
        Logger.e(e) { "Failed to signal existing instance" }
        false
    }
}

private fun startIpcServer() {
    val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
    ipcServer = server
    portFile.writeText(server.localPort.toString())
    portFile.deleteOnExit()
    val thread = Thread({
        while (true) {
            try {
                server.accept().use { it.getInputStream().read() }
                requestForeground()
            } catch (e: Exception) {
                Logger.w(e) { "IPC server stopped" }
                break
            }
        }
    }, "ipc-server")
    thread.isDaemon = true
    thread.start()
}

private val MIN_WIDTH = 400.dp
private val MIN_HEIGHT = 300.dp
private val DEFAULT_WIDTH = 800.dp
private val DEFAULT_HEIGHT = 600.dp
// Only a backstop against a save that never returns. Expiring it kills the JVM mid-write, so it has
// to be longer than a save that reaches the calendar provider and sync adapters can plausibly take.
private const val SHUTDOWN_SAVE_TIMEOUT_MS = 10_000L

@OptIn(FlowPreview::class)
fun main() {
    if (!acquireLock()) {
        signalExistingInstance()
        return
    }
    startIpcServer()
    org.tasks.caldav.CaldavSynchronizer.registerFactories()
    Logger.setMinSeverity(if (TasksBuildConfig.DEBUG) Severity.Verbose else Severity.Debug)
    Logger.setLogWriters(
        buildList {
            if (TasksBuildConfig.DEBUG) add(platformLogWriter())
            add(FileLogWriter(logDir))
        }
    )
    logStartup()

    startKoin {
        modules(commonModule, platformModule())
    }
    val koin = KoinPlatform.getKoin()
    runBlocking {
        koin.get<AppPreferences>()
            .recordInstallIfNeeded(koin.get<PlatformConfiguration>().versionCode)
    }
    // Cmd+Q on macOS goes through here rather than through the window: the JDK's default quit
    // strategy calls System.exit directly, so no window ever sees a close request and none of the
    // commit-and-wait below in onCloseRequest runs. Closing the windows instead routes it through
    // exactly the same path as clicking the close button.
    if (Desktop.isDesktopSupported()) {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.APP_QUIT_STRATEGY)) {
            try {
                desktop.setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS)
            } catch (e: Exception) {
                Logger.w(e) { "Failed to set quit strategy" }
            }
        }
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        step(TAG, "commit pending saves") {
            val pendingSaves = koin.get<PendingTaskSaves>()
            pendingSaves.flushPending()
            runBlocking {
                if (withTimeoutOrNull(SHUTDOWN_SAVE_TIMEOUT_MS) { pendingSaves.awaitIdle() } == null) {
                    Logger.w { "Timed out waiting for pending saves during shutdown" }
                }
            }
        }
        step(TAG, "close the Microsoft client") {
            (koin.get<MicrosoftClientProvider>() as? DesktopMicrosoftClientProvider)?.close()
        }
        step(TAG, "flush cookies") { runBlocking { EncryptedCookieStore.flushAll() } }
        step(TAG, "close reporting") { (koin.get<Reporting>() as? PostHogReporting)?.close() }
        step(TAG, "release the single instance lock") {
            ipcServer?.close()
            portFile.delete()
            lockChannel?.close()
            lockFile.delete()
        }
    })

    application {
        val preferences = koinInject<TasksPreferences>()
        val pendingSaves = koinInject<PendingTaskSaves>()
        val taskRequests = koinInject<TaskRequests>()
        val shutdownScope = rememberCoroutineScope()
        var closing by remember { mutableStateOf(false) }
        val windowState = rememberWindowState(size = DpSize(DEFAULT_WIDTH, DEFAULT_HEIGHT))
        var windowReady by remember { mutableStateOf(false) }
        // Restore saved window size and position before showing the window
        LaunchedEffect(Unit) {
            val w = preferences.get(TasksPreferences.windowWidth, 0)
            val h = preferences.get(TasksPreferences.windowHeight, 0)
            if (w > 0 && h > 0) {
                windowState.size = DpSize(
                    maxOf(w.dp, MIN_WIDTH),
                    maxOf(h.dp, MIN_HEIGHT),
                )
            }
            val x = preferences.get(TasksPreferences.windowX, Int.MIN_VALUE)
            val y = preferences.get(TasksPreferences.windowY, Int.MIN_VALUE)
            if (x != Int.MIN_VALUE && y != Int.MIN_VALUE) {
                windowState.position = WindowPosition(x.dp, y.dp)
            }
            windowReady = true
        }
        // Persist window size and position on changes
        LaunchedEffect(Unit) {
            snapshotFlow { windowState.size to windowState.position }
                .drop(1)
                .debounce(500)
                .collect { (size, position) ->
                    preferences.set(TasksPreferences.windowWidth, size.width.value.toInt())
                    preferences.set(TasksPreferences.windowHeight, size.height.value.toInt())
                    if (position is WindowPosition.Absolute) {
                        preferences.set(TasksPreferences.windowX, position.x.value.toInt())
                        preferences.set(TasksPreferences.windowY, position.y.value.toInt())
                    }
                }
        }
        Window(
            // This fires while the composition is still alive, so no editor has been cleared or
            // stopped and nothing is enqueued yet: ask any open editor to commit first, then give
            // that save a moment to land before tearing the JVM down.
            onCloseRequest = {
                if (!closing) {
                    closing = true
                    setQuitting(true)
                    taskRequests.acceptOpenRequests(false)
                    // The monotonic count, not the one the snackbar acknowledges: that one goes
                    // down too, and App's snackbar loop is still running while this waits below - so
                    // an acknowledgement landing in between made a real shutdown failure compare
                    // equal to the snapshot, and the quit went ahead with the edit lost.
                    val failuresBefore = pendingSaves.totalSaveFailures.value
                    pendingSaves.flushPending()
                    shutdownScope.launch {
                        if (withTimeoutOrNull(SHUTDOWN_SAVE_TIMEOUT_MS) { pendingSaves.awaitIdle() } == null) {
                            Logger.w { "Timed out waiting for pending saves, exiting anyway" }
                        }
                        // A failed save has nowhere to be reported once the process is gone: the
                        // window is already hidden and the count it publishes only ever reaches a
                        // snackbar. So the quit is abandoned instead, and the window comes back with
                        // the edit still in it and the error on screen.
                        if (pendingSaves.totalSaveFailures.value > failuresBefore) {
                            Logger.w { "Save failed while closing, staying open to report it" }
                            // The composition is still running behind the hidden window, so the
                            // snackbar it owns can have shown and acknowledged this failure to an
                            // empty screen while we waited. Re-report it if nothing is owed any
                            // more, or the window comes back with no explanation on it.
                            if (pendingSaves.saveFailures.value == 0) {
                                pendingSaves.reportSaveFailure()
                            }
                            closing = false
                            setQuitting(false)
                            taskRequests.acceptOpenRequests(true)
                            return@launch
                        }
                        exitApplication()
                    }
                }
            },
            title = "Tasks",
            state = windowState,
            // Hidden the moment the user asks to quit. The flush above has already read what the
            // open editor was holding and there is no second one, so anything typed after it would
            // be lost silently - and a save slow enough to notice would read as a hung window.
            visible = windowReady && !closing,
        ) {
            window.minimumSize = Dimension(MIN_WIDTH.value.toInt(), MIN_HEIGHT.value.toInt())
            val reporting = koinInject<Reporting>()
            val sseClient = koinInject<SseClient>()
            val backgroundWork = koinInject<BackgroundWork>()
            val platformConfig = koinInject<PlatformConfiguration>()
            val lifecycleScope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
                    reporting.reportException(throwable, fatal = true)
                }
                val versionCode = platformConfig.versionCode
                if (versionCode > 0) {
                    preferences.set(TasksPreferences.currentVersion, versionCode)
                }
                reporting.logEvent(
                    AnalyticsEvents.APP_OPENED,
                    AnalyticsEvents.PARAM_FROM_BACKGROUND to false,
                )
                sseClient.start()
            }
            DisposableEffect(window) {
                var backgrounded = false
                val focusListener = object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent?) {
                        if (backgrounded) {
                            backgrounded = false
                            reporting.logEvent(
                                AnalyticsEvents.APP_OPENED,
                                AnalyticsEvents.PARAM_FROM_BACKGROUND to true,
                            )
                            sseClient.reconnect()
                            lifecycleScope.launch {
                                backgroundWork.sync(SyncSource.APP_RESUME)
                            }
                        }
                    }

                    override fun windowLostFocus(e: WindowEvent?) {
                        if (!backgrounded) {
                            backgrounded = true
                            reporting.logEvent(AnalyticsEvents.APP_BACKGROUNDED)
                        }
                    }
                }
                window.addWindowFocusListener(focusListener)
                onDispose {
                    window.removeWindowFocusListener(focusListener)
                }
            }
            val userDecisionRegistry = koinInject<DesktopUserDecisionRegistry>()
            TrustCertificateDialog(userDecisionRegistry)
            val serverEnv = koinInject<TasksServerEnvironment>()
            val scope = rememberCoroutineScope()
            var currentEnv by remember { mutableStateOf(serverEnv.currentEnvironment) }
            StableWindowSize {
                App(
                    openUrl = { url ->
                        openInBrowser(url)
                    },
                    environments = serverEnv.environments,
                    currentEnvironment = currentEnv,
                    onSelectEnvironment = { env ->
                        scope.launch {
                            serverEnv.setEnvironment(env)
                            currentEnv = env
                        }
                    },
                )
            }
        }
    }
}
