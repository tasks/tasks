package org.tasks.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpCallsTest {
    private var request: HttpRequestData? = null

    private val factory = object : KtorClientFactory {
        override suspend fun newClient(
            foreground: Boolean,
            cookieKey: String?,
            block: HttpClientConfig<*>.() -> Unit,
        ) = HttpClient(MockEngine {
            request = it
            respond("{}", HttpStatusCode.OK)
        }) { block() }
    }

    @Test
    fun everyStageOfACallGetsTheWholeBudget() = runTest {
        factory.withClient(callTimeoutMillis = 4_000L) { it.get("https://example.com/") }

        val timeouts = request!!.getCapabilityOrNull(HttpTimeoutCapability)!!
        assertEquals(4_000L, timeouts.requestTimeoutMillis)
        assertEquals(4_000L, timeouts.connectTimeoutMillis)
        assertEquals(4_000L, timeouts.socketTimeoutMillis)
    }
}
