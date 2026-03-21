import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.tasks.App
import org.tasks.auth.TasksServerEnvironment
import org.tasks.di.commonModule
import org.tasks.di.platformModule
import java.awt.Desktop
import java.net.URI

fun main() = application {
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
