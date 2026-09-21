package org.tasks.caldav

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TasksBasicAuthTest {
    private var request: HttpRequestData? = null

    private fun client(
        auth: TasksBasicAuth,
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "",
    ) = HttpClient(MockEngine { req ->
        request = req
        respond(body, status, headersOf("Content-Type", "application/json"))
    }) {
        install(auth)
    }

    @Test
    fun sendsCredentialsAndTasksHeaders() = runTest {
        client(
            TasksBasicAuth(
                user = "alice",
                token = "s3cret",
                tosVersion = 3,
                pushToken = "push-1",
                subscriptionInfo = TasksBasicAuth.SubscriptionInfo("annual", "purchase-1"),
            )
        ).get("https://caldav.example.com/")

        val headers = request!!.headers
        assertEquals("Basic YWxpY2U6czNjcmV0", headers["Authorization"])
        assertEquals("3", headers["tasks-tos-version"])
        assertEquals("push-1", headers["X-Push-Token"])
        assertEquals("annual", headers["tasks-sku"])
        assertEquals("purchase-1", headers["tasks-token"])
    }

    @Test
    fun omitsOptionalHeaders() = runTest {
        client(TasksBasicAuth(user = "alice", token = "s3cret", tosVersion = 0)).get("https://caldav.example.com/")

        val headers = request!!.headers
        assertNull(headers["X-Push-Token"])
        assertNull(headers["tasks-sku"])
        assertNull(headers["tasks-token"])
    }

    @Test
    fun purchaseTokenInUseBecomesAnException() = runTest {
        val client = client(
            TasksBasicAuth(user = "alice", token = "s3cret", tosVersion = 0),
            status = HttpStatusCode.PaymentRequired,
            body = """{"error":"purchase_token_in_use","existing_account":"bob@example.com"}""",
        )

        val e = assertFailsWith<PurchaseTokenInUseException> { client.get("https://caldav.example.com/") }
        assertEquals("bob@example.com", e.existingAccount)
    }

    @Test
    fun otherPaymentRequiredResponsesKeepTheirBody() = runTest {
        val client = client(
            TasksBasicAuth(user = "alice", token = "s3cret", tosVersion = 0),
            status = HttpStatusCode.PaymentRequired,
            body = """{"error":"subscription_expired"}""",
        )

        val response = client.get("https://caldav.example.com/")
        assertEquals(402, response.status.value)
        assertEquals("""{"error":"subscription_expired"}""", response.bodyAsText())
    }
}
