package org.tasks.auth

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

data class OAuthConfig(
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val clientId: String,
    val redirectUri: String,
    val scope: String,
    val state: String = "",
)

data class OAuthResult(
    val accessToken: String,
    val idToken: IdToken? = null,
    val refreshToken: String? = null,
    val tokenEndpoint: String? = null,
    val clientId: String? = null,
    val expiresIn: Long? = null,
    val grantedScopes: String? = null,
)

class TasksOAuthClient(
    private val httpClient: HttpClient = defaultHttpClient,
) {
    companion object {
        private val defaultHttpClient: HttpClient by lazy {
            HttpClient {
                install(HttpTimeout) {
                    connectTimeoutMillis = 15_000
                    requestTimeoutMillis = 15_000
                }
            }
        }
    }

    suspend fun fetchDiscovery(discoveryUrl: String, authHeader: String? = null): JsonObject {
        Logger.d("TasksOAuthClient") { "Fetching: $discoveryUrl" }
        val response = httpClient.get(discoveryUrl) {
            authHeader?.let { header(HttpHeaders.Authorization, it) }
        }
        val body = response.bodyAsText()
        Logger.d("TasksOAuthClient") { "Discovery response: ${response.status.value}" }
        if (!response.status.isSuccess()) {
            throw Exception("Discovery request failed: ${response.status.value} $body")
        }
        return Json.parseToJsonElement(body) as JsonObject
    }

    fun buildAuthUrl(
        config: OAuthConfig,
        codeChallenge: String?,
        state: String,
        extraParams: Map<String, String> = emptyMap(),
    ): String {
        fun encode(value: String) = value.encodeURLParameter(spaceToPlus = true)
        val base = "${config.authorizationEndpoint}?" +
            "client_id=${encode(config.clientId)}" +
            "&redirect_uri=${encode(config.redirectUri)}" +
            "&response_type=code" +
            "&scope=${encode(config.scope)}" +
            (codeChallenge?.let { "&code_challenge=${encode(it)}&code_challenge_method=S256" } ?: "") +
            "&state=${encode(state)}"
        val extra = extraParams.entries.joinToString("") { (k, v) ->
            "&${encode(k)}=${encode(v)}"
        }
        return base + extra
    }

    suspend fun exchangeCode(
        config: OAuthConfig,
        code: String,
        codeVerifier: String?,
        authHeader: String? = null,
    ): OAuthResult = requestToken(
        tokenEndpoint = config.tokenEndpoint,
        clientId = config.clientId,
        form = buildMap {
            put("grant_type", "authorization_code")
            put("client_id", config.clientId)
            put("redirect_uri", config.redirectUri)
            put("code", code)
            codeVerifier?.let { put("code_verifier", it) }
        },
        authHeader = authHeader,
    )

    suspend fun exchangeAppleCredential(
        tokenEndpoint: String,
        clientId: String,
        authorizationCode: String,
        identityToken: String,
        nonce: String,
        email: String? = null,
        fullName: String? = null,
        authHeader: String? = null,
    ): OAuthResult = requestToken(
        tokenEndpoint = tokenEndpoint,
        clientId = clientId,
        form = buildMap {
            put("grant_type", "authorization_code")
            put("client_id", clientId)
            put("code", authorizationCode)
            put("id_token", identityToken)
            put("nonce", nonce)
            email?.let { put("email", it) }
            fullName?.let { put("name", it) }
        },
        authHeader = authHeader,
    )

    private suspend fun requestToken(
        tokenEndpoint: String,
        clientId: String,
        form: Map<String, String>,
        authHeader: String?,
    ): OAuthResult {
        val response = post(tokenEndpoint, form, authHeader)
        val body = response.bodyAsText()

        if (!response.status.isSuccess()) {
            Logger.e("TasksOAuthClient") { "${TokenError.EXCHANGE_FAILED}: ${response.status.value} $body" }
            throw tokenErrorException(TokenError.EXCHANGE_FAILED, response.status.value, body)
        }

        val json = Json.parseToJsonElement(body) as JsonObject
        val accessToken = json["access_token"]?.jsonPrimitive?.content
            ?: throw Exception("No access_token in response")
        val idTokenStr = json["id_token"]?.jsonPrimitive?.content
        val refreshToken = json["refresh_token"]?.jsonPrimitive?.content
        val expiresIn = json["expires_in"]?.jsonPrimitive?.content?.toLongOrNull()
        val grantedScopes = json["scope"]?.jsonPrimitive?.content
        Logger.d("TasksOAuthClient") { "Token exchange granted scopes: $grantedScopes" }

        return OAuthResult(
            accessToken = accessToken,
            idToken = idTokenStr?.let { IdToken(it) },
            refreshToken = refreshToken,
            tokenEndpoint = tokenEndpoint,
            clientId = clientId,
            expiresIn = expiresIn,
            grantedScopes = grantedScopes,
        )
    }

    data class RefreshResult(
        val accessToken: String,
        val expiresIn: Long?,
        val refreshToken: String? = null,
    )

    suspend fun refreshToken(
        tokenEndpoint: String,
        clientId: String,
        refreshToken: String,
        authHeader: String? = null,
    ): RefreshResult {
        val response = post(
            tokenEndpoint,
            mapOf(
                "grant_type" to "refresh_token",
                "client_id" to clientId,
                "refresh_token" to refreshToken,
            ),
            authHeader,
        )
        val body = response.bodyAsText()

        if (!response.status.isSuccess()) {
            Logger.e("TasksOAuthClient") { "${TokenError.REFRESH_FAILED}: ${response.status.value} $body" }
            throw tokenErrorException(TokenError.REFRESH_FAILED, response.status.value, body)
        }

        val json = Json.parseToJsonElement(body) as JsonObject
        val accessToken = json["access_token"]?.jsonPrimitive?.content
            ?: throw Exception("No access_token in refresh response")
        val expiresIn = json["expires_in"]?.jsonPrimitive?.content?.toLongOrNull()
        val rotatedRefreshToken = json["refresh_token"]?.jsonPrimitive?.content
        return RefreshResult(
            accessToken = accessToken,
            expiresIn = expiresIn,
            refreshToken = rotatedRefreshToken,
        )
    }

    private suspend fun post(url: String, form: Map<String, String>, authHeader: String?): HttpResponse =
        httpClient.submitForm(url, parameters { form.forEach { (name, value) -> append(name, value) } }) {
            header(HttpHeaders.Accept, "application/json")
            authHeader?.let { header(HttpHeaders.Authorization, it) }
        }
}

private fun tokenErrorException(prefix: String, code: Int, body: String): Exception {
    val (error, description) = parseOAuthError(body)
    ConditionalAccess.devicePolicyException(error, description)?.let { return it }
    val detail = description ?: error
    val transient = code == 429 || code in 500..599
    return if (transient) {
        Exception(if (detail != null) "Token endpoint unavailable: $detail" else "Token endpoint unavailable: $code")
    } else {
        Exception(if (detail != null) "$prefix: $detail" else "$prefix: $code")
    }
}

private fun parseOAuthError(body: String): Pair<String?, String?> = try {
    val json = Json.parseToJsonElement(body) as JsonObject
    val error = json["error"]?.jsonPrimitive?.content
    val description = json["error_description"]?.jsonPrimitive?.content
    error to description
} catch (_: Exception) {
    null to null
}
