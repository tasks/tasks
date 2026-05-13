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
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import org.tasks.TasksBuildConfig
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.PostHogReporting
import org.tasks.analytics.Reporting
import org.tasks.App
import org.tasks.auth.TasksServerEnvironment
import org.tasks.jobs.BackgroundWork
import org.tasks.PlatformConfiguration
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.recordInstallIfNeeded
import org.tasks.sse.SseClient
import org.tasks.sync.SyncSource
import org.tasks.di.commonModule
import org.tasks.di.dataDir
import org.tasks.di.platformModule
import org.tasks.logging.FileLogWriter
import java.awt.Dimension
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File
import org.tasks.extensions.openInBrowser

private val MIN_WIDTH = 400.dp
private val MIN_HEIGHT = 300.dp
private val DEFAULT_WIDTH = 800.dp
private val DEFAULT_HEIGHT = 600.dp

@OptIn(FlowPreview::class)
fun main() {
    org.tasks.caldav.CaldavSynchronizer.registerFactories()
    val logDir = File(dataDir(), "logs").apply { mkdirs() }
    Logger.setLogWriters(
        buildList {
            if (TasksBuildConfig.DEBUG) add(platformLogWriter())
            add(FileLogWriter(logDir))
        }
    )

    startKoin {
        modules(commonModule, platformModule())
    }
    val koin = KoinPlatform.getKoin()
    runBlocking {
        koin.get<AppPreferences>()
            .recordInstallIfNeeded(koin.get<PlatformConfiguration>().versionCode)
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        (koin.get<Reporting>() as? PostHogReporting)?.close()
    })

    application {
        val preferences = koinInject<TasksPreferences>()
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
            onCloseRequest = ::exitApplication,
            title = "Tasks",
            state = windowState,
            visible = windowReady,
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
            val serverEnv = koinInject<TasksServerEnvironment>()
            val scope = rememberCoroutineScope()
            var currentEnv by remember { mutableStateOf(serverEnv.currentEnvironment) }
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
