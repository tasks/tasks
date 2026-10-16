package org.tasks.billing

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.tasks.http.KtorClientFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StripeClientTest {
    private val requests = mutableListOf<HttpRequestData>()
    private var handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respondOk("{}") }

    private val factory = object : KtorClientFactory {
        override suspend fun newClient(
            foreground: Boolean,
            cookieKey: String?,
            block: HttpClientConfig<*>.() -> Unit,
        ) = HttpClient(MockEngine { request ->
            requests.add(request)
            handler(request)
        }) { block() }
    }

    private var caldavUrl = "https://caldav.example.com"

    private fun client(authorization: String? = null) = StripeClientImpl(
        httpClientFactory = factory,
        caldavUrl = { caldavUrl },
        json = Json { ignoreUnknownKeys = true },
        authorization = { authorization },
    )

    private fun respondWith(
        status: HttpStatusCode,
        body: String,
    ) {
        handler = { respond(body, status, headersOf("Content-Type", "application/json")) }
    }

    private fun MockRequestHandleScope.respondOk(body: String) =
        respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))

    private fun body(index: Int = 0) = (requests[index].body as TextContent).text

    @Test
    fun takesTheServerFromEveryCallRatherThanTheOneItWasBuiltWith() = runTest {
        respondWith(HttpStatusCode.OK, """{"prices":[]}""")
        val client = client()

        client.prices()
        caldavUrl = "https://staging.example.com"
        client.prices()

        assertEquals("https://caldav.example.com/stripe/prices", requests[0].url.toString())
        assertEquals("https://staging.example.com/stripe/prices", requests[1].url.toString())
    }

    @Test
    fun readsTheLadder() = runTest {
        respondWith(
            HttpStatusCode.OK,
            """{"prices":[""" +
                """{"price_id":"pri_01","sku":"monthly_01","amount_cents":100,"currency":"USD",""" +
                """"interval":"month","frequency":1,"entitles":false},""" +
                """{"price_id":"pri_03","sku":"monthly_03","amount_cents":300,"currency":"USD",""" +
                """"interval":"month","frequency":1,"entitles":true},""" +
                """{"price_id":"pri_30","sku":"annual_30","amount_cents":3000,"currency":"USD",""" +
                """"interval":"year","frequency":1,"entitles":true}]}""",
        )

        val prices = client().prices()

        assertEquals("https://caldav.example.com/stripe/prices", requests[0].url.toString())
        assertEquals(
            listOf(
                StripePrice("pri_01", "monthly_01", 100, "USD", "month", false),
                StripePrice("pri_03", "monthly_03", 300, "USD", "month", true),
                StripePrice("pri_30", "annual_30", 3000, "USD", "year", true),
            ),
            prices,
        )
    }

    @Test
    fun anEmptyLadderIsAnAnswer() = runTest {
        respondWith(HttpStatusCode.OK, """{"prices":[]}""")

        assertEquals(emptyList(), client().prices())
    }

    @Test
    fun hasNoLadderWhenPricesAreUnavailable() = runTest {
        handler = { respondError(HttpStatusCode.ServiceUnavailable) }

        assertNull(client().prices())
    }

    @Test
    fun hasNoLadderWhenThePayloadIsNotOne() = runTest {
        respondWith(HttpStatusCode.OK, """["monthly_01"]""")

        assertNull(client().prices())
    }

    @Test
    fun opensACheckoutForAPriceId() = runTest {
        respondWith(
            HttpStatusCode.OK,
            """{"code":"c0de","url":"https://pay.stripe.com/x","expires_at":1000}""",
        )

        val result = client().createCheckout("pri_2")

        assertEquals("https://caldav.example.com/stripe/checkout", requests[0].url.toString())
        assertEquals("""{"price_id":"pri_2"}""", body())
        assertEquals(StripeCheckout.Started("c0de", "https://pay.stripe.com/x", 1000), result)
    }

    @Test
    fun letsTheServerPickTheDefaultPriceWhenNoneIsNamed() = runTest {
        respondWith(
            HttpStatusCode.OK,
            """{"code":"c0de","url":"https://pay.stripe.com/x","expires_at":1000}""",
        )

        client().createCheckout(null)

        assertEquals(1, requests.size)
        assertEquals("{}", body())
    }

    @Test
    fun sendsTheSessionWhenSignedIn() = runTest {
        respondWith(
            HttpStatusCode.OK,
            """{"code":"c0de","url":"https://pay.stripe.com/x","expires_at":1000}""",
        )

        client(authorization = "Basic abc").createCheckout(null)

        assertEquals("Basic abc", requests[0].headers["Authorization"])
    }

    @Test
    fun buysAnonymouslyWhenTheSessionIsGone() = runTest {
        handler = { request ->
            if (request.headers["Authorization"] != null) {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                respondOk("""{"code":"c0de","url":"https://pay.stripe.com/x","expires_at":1000}""")
            }
        }

        val result = client(authorization = "Basic stale").createCheckout(null)

        assertEquals(StripeCheckout.Started("c0de", "https://pay.stripe.com/x", 1000), result)
        assertEquals(2, requests.size)
        assertNull(requests[1].headers["Authorization"])
    }

    @Test
    fun readsTheTrialOffTheLadder() = runTest {
        respondWith(
            HttpStatusCode.OK,
            """{"prices":[{"price_id":"pri_1","sku":"monthly_03","amount_cents":299,""" +
                """"currency":"USD","interval":"month","frequency":1,"trial_days":7,""" +
                """"entitles":true}]}""",
        )

        val price = client().prices()?.single()

        assertEquals(7, price?.trialDays)
        assertEquals(299, price?.amountCents)
        assertEquals("monthly_03", price?.sku)
    }

    @Test
    fun aTierWithoutATrialSaysNothingAboutOne() = runTest {
        respondWith(
            HttpStatusCode.OK,
            """{"prices":[{"price_id":"pri_1","sku":"annual_01","amount_cents":100}]}""",
        )

        assertEquals(0, client().prices()?.single()?.trialDays)
    }

    @Test
    fun refusesASecondSubscription() = runTest {
        respondWith(
            HttpStatusCode.Conflict,
            """{"error":"subscription_exists","product":"monthly_02","status":"active"}""",
        )

        assertEquals(
            StripeCheckout.SubscriptionExists(product = "monthly_02"),
            client().createCheckout(null),
        )
    }

    @Test
    fun treatsAnUnnamedConflictAsAnExistingSubscription() = runTest {
        respondWith(HttpStatusCode.Conflict, """{"error":"something_else"}""")

        assertEquals(StripeCheckout.SubscriptionExists(), client().createCheckout(null))
    }

    @Test
    fun mapsCheckoutFailures() = runTest {
        respondWith(HttpStatusCode.BadRequest, """{"error":"unknown_price"}""")
        assertEquals(StripeCheckout.UnknownPrice, client().createCheckout(null))

        respondWith(HttpStatusCode.TooManyRequests, "")
        assertEquals(StripeCheckout.RateLimited, client().createCheckout(null))

        respondWith(HttpStatusCode.ServiceUnavailable, """{"error":"stripe_unavailable"}""")
        assertEquals(StripeCheckout.Unavailable, client().createCheckout(null))

        respondWith(HttpStatusCode.InternalServerError, "")
        assertEquals(StripeCheckout.Failed, client().createCheckout(null))
    }

    @Test
    fun keepsPollingWhileTheUserIsAtTheTill() = runTest {
        respondWith(HttpStatusCode.OK, """{"status":"pending"}""")

        assertEquals(StripeCheckoutStatus.Pending, client().pollCheckout("c0de"))
        assertEquals("https://caldav.example.com/stripe/checkout/c0de", requests[0].url.toString())
    }

    @Test
    fun readsTheCompletedPurchase() = runTest {
        respondWith(
            HttpStatusCode.OK,
            """{"status":"complete","purchase_jwt":"jwt","refresh_token":"rt",""" +
                """"sku":"monthly_stripe","formatted_price":"${'$'}3.00"}""",
        )

        assertEquals(
            StripeCheckoutStatus.Complete(
                StripePurchase("jwt", "rt", "monthly_stripe", "\$3.00")
            ),
            client().pollCheckout("c0de"),
        )
    }

    @Test
    fun treatsACompletionWithoutTokensAsAFailure() = runTest {
        respondWith(HttpStatusCode.OK, """{"status":"complete"}""")

        assertEquals(StripeCheckoutStatus.Failed, client().pollCheckout("c0de"))
    }

    @Test
    fun mapsPollFailures() = runTest {
        respondWith(HttpStatusCode.NotFound, """{"error":"Checkout not found or expired"}""")
        assertEquals(StripeCheckoutStatus.Gone, client().pollCheckout("c0de"))

        respondWith(HttpStatusCode.PaymentRequired, """{"error":"Invalid or expired subscription"}""")
        assertEquals(StripeCheckoutStatus.SubscriptionInvalid, client().pollCheckout("c0de"))

        respondWith(HttpStatusCode.ServiceUnavailable, """{"error":"stripe_unavailable"}""")
        assertEquals(StripeCheckoutStatus.Unavailable, client().pollCheckout("c0de"))

        respondWith(HttpStatusCode.InternalServerError, "")
        assertEquals(StripeCheckoutStatus.Failed, client().pollCheckout("c0de"))
    }

    @Test
    fun readsHowLongTheServerWantsTheClientToWait() = runTest {
        handler = {
            respond(
                """{"error":"rate_limited"}""",
                HttpStatusCode.TooManyRequests,
                headersOf("Retry-After", listOf("20")),
            )
        }

        assertEquals(
            StripeCheckoutStatus.RateLimited(retryAfterMillis = 20_000L),
            client().pollCheckout("c0de"),
        )
    }

    @Test
    fun aRateLimitWithNoRetryAfterLeavesTheWaitToTheCaller() = runTest {
        respondWith(HttpStatusCode.TooManyRequests, """{"error":"rate_limited"}""")

        assertEquals(
            StripeCheckoutStatus.RateLimited(retryAfterMillis = null),
            client().pollCheckout("c0de"),
        )
    }

    @Test
    fun aCallThatNeverArrivedIsUnavailable() = runTest {
        handler = { throw RuntimeException("no network") }

        assertEquals(StripeCheckout.Unavailable, client().createCheckout(null))
        assertEquals(StripeCheckoutStatus.Unavailable, client().pollCheckout("c0de"))
        assertNull(client().prices())
    }

    @Test
    fun anAnswerWeDidNotExpectIsAFailure() = runTest {
        respondWith(HttpStatusCode.InternalServerError, "")

        assertEquals(StripeCheckout.Failed, client().createCheckout(null))
        assertEquals(StripeCheckoutStatus.Failed, client().pollCheckout("c0de"))
    }
}
