package org.tasks.billing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.tasks.auth.TasksServerEnvironment
import org.tasks.http.OkHttpClientFactory

class DesktopLinkClientImpl(
    private val httpClientFactory: OkHttpClientFactory,
    private val serverEnvironment: TasksServerEnvironment,
    private val desktopEntitlement: DesktopEntitlement,
    private val json: Json,
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

    override suspend fun createLink(): LinkResult? = withContext(Dispatchers.IO) {
        try {
            val client = httpClientFactory.newClient()
            val request = Request.Builder()
                .url("${serverEnvironment.caldavUrl}/desktop/link")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val result = json.decodeFromString(LinkResponse.serializer(), body)
                    LinkResult(
                        code = result.code,
                        expiresAt = result.expires_at,
                    )
                } else {
                    logger.w { "Failed to create link: ${response.code}" }
                    null
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to create link" }
            null
        }
    }

    override suspend fun pollStatus(code: String): StatusResult? = withContext(Dispatchers.IO) {
        try {
            val client = httpClientFactory.newClient()
            val request = Request.Builder()
                .url("${serverEnvironment.caldavUrl}/desktop/status/$code")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val result = json.decodeFromString(StatusResponse.serializer(), body)
                    StatusResult(
                        status = result.status,
                        jwt = result.jwt,
                        refreshToken = result.refresh_token,
                        sku = result.sku,
                        formattedPrice = result.formatted_price,
                    )
                } else {
                    logger.w { "Failed to poll status: ${response.code}" }
                    null
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to poll status" }
            null
        }
    }

    override suspend fun onLinkSuccess(jwt: String, refreshToken: String, sku: String?, formattedPrice: String?) {
        desktopEntitlement.storeEntitlement(jwt, refreshToken, sku, formattedPrice, EntitlementProvider.PLAY)
    }
}
