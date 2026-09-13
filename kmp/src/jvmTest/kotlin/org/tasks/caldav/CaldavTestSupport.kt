package org.tasks.caldav

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import org.tasks.InMemoryDataStore
import org.tasks.auth.TasksServerEnvironment
import org.tasks.http.OkHttpClientFactory
import org.tasks.preferences.TasksPreferences
import org.tasks.security.KeyProvider
import org.tasks.security.KeyStoreEncryption
import javax.crypto.KeyGenerator

fun testEncryption() = KeyStoreEncryption(object : KeyProvider {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    override fun getKey() = key
})

fun testClientProvider(
    preferences: TasksPreferences = TasksPreferences(InMemoryDataStore()),
    encryption: KeyStoreEncryption = testEncryption(),
) = CaldavClientProvider(
    encryption = encryption,
    tasksPreferences = preferences,
    environment = TasksServerEnvironment(preferences),
    httpClientFactory = object : OkHttpClientFactory {
        override suspend fun newClient(
            foreground: Boolean,
            cookieKey: String?,
            block: (OkHttpClient.Builder) -> Unit,
        ) = OkHttpClient.Builder().followRedirects(false).apply(block).build()
    },
)

fun failFastServer() = MockWebServer().apply { (dispatcher as QueueDispatcher).setFailFast(true) }

fun multiStatus(body: String) = MockResponse()
    .setResponseCode(207)
    .setHeader("Content-Type", "text/xml; charset=\"utf-8\"")
    .setBody(body)
