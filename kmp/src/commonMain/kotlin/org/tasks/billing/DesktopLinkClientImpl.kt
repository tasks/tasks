package org.tasks.billing

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tasks.billing.PurchaseEntitlement.StoreResult
import org.tasks.http.KtorClientFactory
import org.tasks.http.withClient

class DesktopLinkClientImpl(
    private val httpClientFactory: KtorClientFactory,
    private val caldavUrl: () -> String,
    private val json: Json,
    private val storeEntitlement: suspend (
        jwt: String,
        refreshToken: String,
        sku: String?,
        formattedPrice: String?,
    ) -> StoreResult,
    private val callTimeoutMillis: Long = CALL_TIMEOUT_MILLIS,
) : DesktopLinkClient {
    private val logger = Logger.withTag("DesktopLinkClient")

    @Serializable
    private data class LinkResponse(
        val code: String,
        val expires_at: Long = 0L,
    )

    @Serializable
    private data class StatusResponse(
        val status: String = "pending",
        val jwt: String? = null,
        val refresh_token: String? = null,
        val sku: String? = null,
        val formatted_price: String? = null,
    )

    override suspend fun createLink(): LinkResult? = call("create link") { client ->
        val response = client.post("${caldavUrl()}/desktop/link") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        if (response.status.isSuccess()) {
            val result = json.decodeFromString(LinkResponse.serializer(), response.bodyAsText())
            LinkResult(code = result.code, expiresAt = result.expires_at)
        } else {
            logger.w { "Failed to create link: ${response.status.value}" }
            null
        }
    }

    override suspend fun pollStatus(code: String): StatusResult? = call("poll status") { client ->
        val response = client.get(caldavUrl()) {
            url { appendPathSegments(listOf("desktop", "status", code), encodeSlash = true) }
        }
        if (response.status.isSuccess()) {
            val result = json.decodeFromString(StatusResponse.serializer(), response.bodyAsText())
            StatusResult(
                status = result.status,
                jwt = result.jwt,
                refreshToken = result.refresh_token,
                sku = result.sku,
                formattedPrice = result.formatted_price,
            )
        } else {
            logger.w { "Failed to poll status: ${response.status.value}" }
            null
        }
    }

    override suspend fun onLinkSuccess(
        jwt: String,
        refreshToken: String,
        sku: String?,
        formattedPrice: String?,
    ): StoreResult = storeEntitlement(jwt, refreshToken, sku, formattedPrice)

    private suspend fun <T> call(
        what: String,
        block: suspend (HttpClient) -> T?,
    ): T? = withContext(Dispatchers.IO) {
        try {
            httpClientFactory.withClient(callTimeoutMillis, block)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Failed to $what" }
            null
        }
    }

    companion object {
        private const val CALL_TIMEOUT_MILLIS = 20_000L
    }
}
