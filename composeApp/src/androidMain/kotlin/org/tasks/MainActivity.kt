package org.tasks

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.tasks.auth.TasksServerEnvironment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoinContext {
                val serverEnv = koinInject<TasksServerEnvironment>()
                val scope = rememberCoroutineScope()
                var currentEnv by remember { mutableStateOf(serverEnv.currentEnvironment) }
                App(
                    openUrl = { url ->
                        val uri = url.toUri()
                        if (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) {
                            CustomTabsIntent.Builder()
                                .build()
                                .launchUrl(this@MainActivity, uri)
                        } else {
                            startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
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
