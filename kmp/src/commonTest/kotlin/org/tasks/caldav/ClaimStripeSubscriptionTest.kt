package org.tasks.caldav

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ClaimStripeSubscriptionTest {
    private var request: HttpRequestData? = null
    private val jwt = "eyJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJwdXJjaGFzZSJ9.c2ln"

    @Test
    fun sendsThePurchaseJwtToTheAccount() = runTest {
        val result = client(
            HttpStatusCode.OK,
            """{"claimed":true,"subscription":{"provider":"stripe","product":"monthly_stripe"}}""",
        ).claimStripeSubscription(jwt)

        assertEquals(StripeSubscriptionResult.Claimed, result)
        assertEquals(HttpMethod.Post, request!!.method)
        assertEquals("/stripe/subscription", request!!.url.encodedPath)
        val body = Json.parseToJsonElement((request!!.body as TextContent).text).jsonObject
        assertEquals(jwt, body["purchase_jwt"]?.jsonPrimitive?.content)
    }

    @Test
    fun aSubscriptionBelowTheCloudFloorIsRecordedNotRejected() = runTest {
        assertEquals(
            StripeSubscriptionResult.InsufficientSubscription,
            client(
                HttpStatusCode.PaymentRequired,
                """{"error":"insufficient_subscription","provider":"stripe"}""",
            ).claimStripeSubscription(jwt),
        )
    }

    @Test
    fun aSubscriptionFundingAnotherAccountNamesIt() = runTest {
        assertEquals(
            StripeSubscriptionResult.TokenInUse("b**@e**.com"),
            client(
                HttpStatusCode.PaymentRequired,
                """{"error":"purchase_token_in_use","provider":"stripe","existing_account":"b**@e**.com"}""",
            ).claimStripeSubscription(jwt),
        )
    }

    @Test
    fun aCancelledSubscriptionIsGone() = runTest {
        assertEquals(
            StripeSubscriptionResult.NoSubscription,
            client(
                HttpStatusCode.PaymentRequired,
                """{"error":"Invalid or expired subscription"}""",
            ).claimStripeSubscription(jwt),
        )
    }

    @Test
    fun aTokenWeDidNotSignNeedsARestore() = runTest {
        assertEquals(
            StripeSubscriptionResult.InvalidPurchaseJwt,
            client(HttpStatusCode.BadRequest, """{"error":"invalid_purchase_jwt"}""")
                .claimStripeSubscription(jwt),
        )
    }

    @Test
    fun anEvictedDeviceSlotNeedsARestore() = runTest {
        assertEquals(
            StripeSubscriptionResult.UnknownPurchase,
            client(HttpStatusCode.NotFound, """{"error":"unknown_purchase"}""")
                .claimStripeSubscription(jwt),
        )
    }

    @Test
    fun anAccountCanOnlyHoldOneCustomer() = runTest {
        assertEquals(
            StripeSubscriptionResult.AlreadyLinked,
            client(HttpStatusCode.Conflict, """{"error":"already_linked"}""")
                .claimStripeSubscription(jwt),
        )
    }

    @Test
    fun anUnreachableStripeIsWorthRetrying() = runTest {
        assertEquals(
            StripeSubscriptionResult.Unavailable,
            client(HttpStatusCode.ServiceUnavailable, """{"error":"stripe_unavailable"}""")
                .claimStripeSubscription(jwt),
        )
    }

    @Test
    fun anythingElseIsAFailure() = runTest {
        assertEquals(
            StripeSubscriptionResult.Failed,
            client(HttpStatusCode.InternalServerError, "").claimStripeSubscription(jwt),
        )
    }

    @Test
    fun theSyncGatesOwnPurchaseTokenExceptionIsAnswerEnough() = runTest {
        val client = TasksClient(
            HttpClient(MockEngine { req ->
                request = req
                respond(
                    """{"error":"purchase_token_in_use","existing_account":"b**@e**.com"}""",
                    HttpStatusCode.PaymentRequired,
                    headersOf("Content-Type", "application/json"),
                )
            }) {
                install(TasksBasicAuth(user = "alice", token = "s3cret", tosVersion = 0))
            },
            Url("https://caldav.example.com"),
        )

        assertEquals(
            StripeSubscriptionResult.TokenInUse("b**@e**.com"),
            client.claimStripeSubscription(jwt),
        )
    }

    private fun client(status: HttpStatusCode, body: String) = TasksClient(
        HttpClient(MockEngine { req ->
            request = req
            respond(body, status, headersOf("Content-Type", "application/json"))
        }),
        Url("https://caldav.example.com"),
    )
}
