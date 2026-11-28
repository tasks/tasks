package org.tasks.billing

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.tasks.billing.PurchaseEntitlement.StoreResult
import org.tasks.http.KtorClientFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopLinkClientImplTest {

    private val requests = mutableListOf<HttpRequestData>()
    private val stored = mutableListOf<List<String?>>()

    private fun client(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "{}",
        fail: Boolean = false,
    ) = DesktopLinkClientImpl(
        httpClientFactory = object : KtorClientFactory {
            override suspend fun newClient(
                foreground: Boolean,
                cookieKey: String?,
                block: HttpClientConfig<*>.() -> Unit,
            ) = HttpClient(MockEngine { request ->
                requests += request
                if (fail) throw Exception("offline")
                respond(body, status, headersOf("Content-Type", "application/json"))
            }) { block() }
        },
        caldavUrl = { "https://caldav.tasks.org" },
        json = Json { ignoreUnknownKeys = true },
        storeEntitlement = { jwt, refreshToken, sku, formattedPrice ->
            stored += listOf(jwt, refreshToken, sku, formattedPrice)
            StoreResult.STORED
        },
    )

    @Test
    fun createsALinkCode() = runTest {
        val result = client(body = """{"code":"ABC123","expires_at":1800000000}""").createLink()

        assertEquals(LinkResult(code = "ABC123", expiresAt = 1800000000), result)
        assertEquals(HttpMethod.Post, requests.single().method)
        assertEquals("https://caldav.tasks.org/desktop/link", requests.single().url.toString())
    }

    @Test
    fun aFailedLinkRequestIsNull() = runTest {
        assertNull(client(status = HttpStatusCode.InternalServerError).createLink())
        assertNull(client(fail = true).createLink())
    }

    @Test
    fun pollsAPendingLink() = runTest {
        val result = client(body = """{"status":"pending"}""").pollStatus("ABC123")

        assertEquals("pending", result?.status)
        assertNull(result?.jwt)
        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals("https://caldav.tasks.org/desktop/status/ABC123", requests.single().url.toString())
    }

    @Test
    fun encodesThePolledCode() = runTest {
        client(body = """{"status":"pending"}""").pollStatus("AB/12?x#y")

        assertEquals("https://caldav.tasks.org/desktop/status/AB%2F12%3Fx%23y", requests.single().url.toString())
    }

    @Test
    fun pollsAConfirmedLink() = runTest {
        val result = client(
            body = """{"status":"confirmed","jwt":"j","refresh_token":"r","sku":"annual","formatted_price":"$29.99"}""",
        ).pollStatus("ABC123")

        assertEquals(
            StatusResult(
                status = "confirmed",
                jwt = "j",
                refreshToken = "r",
                sku = "annual",
                formattedPrice = "$29.99",
            ),
            result,
        )
    }

    @Test
    fun aFailedPollIsNull() = runTest {
        assertNull(client(status = HttpStatusCode.NotFound).pollStatus("ABC123"))
        assertNull(client(fail = true).pollStatus("ABC123"))
    }

    @Test
    fun aConfirmedLinkStoresTheEntitlement() = runTest {
        val result = client().onLinkSuccess("j", "r", "annual", "$29.99")

        assertEquals(StoreResult.STORED, result)
        assertEquals<List<List<String?>>>(listOf(listOf("j", "r", "annual", "$29.99")), stored)
    }
}
