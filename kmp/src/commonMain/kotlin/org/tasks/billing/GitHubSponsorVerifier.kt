package org.tasks.billing

import co.touchlab.kermit.Logger
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tasks.auth.TasksServerEnvironment
import org.tasks.billing.PurchaseEntitlement.StoreResult
import org.tasks.http.KtorClientFactory
import org.tasks.http.jsonBody
import org.tasks.http.withClient

class GitHubSponsorVerifier(
    private val httpClientFactory: KtorClientFactory,
    private val serverEnvironment: TasksServerEnvironment,
    private val purchaseEntitlement: PurchaseEntitlement,
    private val json: Json,
    private val installId: suspend () -> String? = { null },
    private val callTimeoutMillis: Long = CALL_TIMEOUT_MILLIS,
) {
    private val logger = Logger.withTag(TAG)

    @Serializable
    private data class VerifyRequest(
        val code: String,
        val redirect_uri: String,
        val install_id: String? = null,
    )

    @Serializable
    private data class VerifyResponse(
        val jwt: String,
        val refresh_token: String,
        val sku: String? = null,
        val formatted_price: String? = null,
    )

    suspend fun verify(
        code: String,
        redirectUri: String,
    ): GitHubSponsorClient.VerifyResult = withContext(Dispatchers.IO) {
        try {
            httpClientFactory.withClient(callTimeoutMillis) { client ->
                val response = client.post("${serverEnvironment.caldavUrl}$VERIFY_PATH") {
                    jsonBody(
                        json,
                        VerifyRequest.serializer(),
                        VerifyRequest(
                            code = code,
                            redirect_uri = redirectUri,
                            install_id = installId(),
                        ),
                    )
                }
                when {
                    response.status.value in 200..299 -> store(
                        json.decodeFromString(VerifyResponse.serializer(), response.bodyAsText())
                    )
                    response.status.value == 402 -> {
                        logger.i { "Not a sponsor" }
                        GitHubSponsorClient.VerifyResult.NotSponsor
                    }
                    else -> {
                        logger.w { "GitHub verify failed: ${response.status.value}" }
                        GitHubSponsorClient.VerifyResult.Failed
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "GitHub verify request failed" }
            GitHubSponsorClient.VerifyResult.Failed
        }
    }

    private suspend fun store(result: VerifyResponse): GitHubSponsorClient.VerifyResult =
        when (
            purchaseEntitlement.storeEntitlement(
                jwt = result.jwt,
                refreshToken = result.refresh_token,
                sku = result.sku,
                formattedPrice = result.formatted_price,
                provider = EntitlementProvider.GITHUB_SPONSOR,
            )
        ) {
            StoreResult.STORED -> GitHubSponsorClient.VerifyResult.Success
            StoreResult.ENCRYPT_FAILED ->
                GitHubSponsorClient.VerifyResult.NotStored(encryptionFailed = true)
            StoreResult.WRITE_FAILED, StoreResult.REJECTED ->
                GitHubSponsorClient.VerifyResult.NotStored(encryptionFailed = false)
        }

    companion object {
        private const val TAG = "GitHubSponsorVerifier"
        private const val VERIFY_PATH = "/desktop/github/verify"
        private const val CALL_TIMEOUT_MILLIS = 20_000L
    }
}
