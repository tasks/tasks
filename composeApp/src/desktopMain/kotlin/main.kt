import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.launch
import org.tasks.kmp.IS_DEBUG
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.tasks.App
import org.tasks.auth.TasksServerEnvironment
import org.tasks.di.commonModule
import org.tasks.di.platformModule
import org.tasks.logging.FileLogWriter
import java.awt.Desktop
import java.io.File
import java.net.URI

fun main() {
    val logDir = File(System.getProperty("user.home"), ".tasks.org/logs").apply { mkdirs() }
    Logger.setLogWriters(
        buildList {
            if (IS_DEBUG) add(platformLogWriter())
            add(FileLogWriter(logDir))
        }
    )

    application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Tasks",
    ) {
        KoinApplication(application = {
            modules(commonModule, platformModule())
        }) {
            val serverEnv = koinInject<TasksServerEnvironment>()
            val scope = rememberCoroutineScope()
            var currentEnv by remember { mutableStateOf(serverEnv.currentEnvironment) }
            App(
                openUrl = { url ->
                    Desktop.getDesktop().browse(URI(url))
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
