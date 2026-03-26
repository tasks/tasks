package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

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
    val idToken: IdToken,
)

class TasksOAuthClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build(),
) {
    fun fetchDiscovery(discoveryUrl: String): JsonObject {
        val request = Request.Builder().url(discoveryUrl).build()
        Logger.d("TasksOAuthClient") { "Fetching: $discoveryUrl" }
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty discovery response")
        Logger.d("TasksOAuthClient") { "Discovery response: ${response.code}" }
        if (!response.isSuccessful) {
            throw Exception("Discovery request failed: ${response.code} $body")
        }
        return Json.parseToJsonElement(body) as JsonObject
    }

    fun buildAuthUrl(config: OAuthConfig, codeChallenge: String, state: String): String {
        fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
        return "${config.authorizationEndpoint}?" +
            "client_id=${encode(config.clientId)}" +
            "&redirect_uri=${encode(config.redirectUri)}" +
            "&response_type=code" +
            "&scope=${encode(config.scope)}" +
            "&code_challenge=${encode(codeChallenge)}" +
            "&code_challenge_method=S256" +
            "&prompt=select_account" +
            "&state=${encode(state)}"
    }

    fun exchangeCode(
        config: OAuthConfig,
        code: String,
        codeVerifier: String,
    ): OAuthResult {
        val formBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", config.clientId)
            .add("redirect_uri", config.redirectUri)
            .add("code", code)
            .add("code_verifier", codeVerifier)
            .build()

        val request = Request.Builder()
            .url(config.tokenEndpoint)
            .post(formBody)
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty token response")
        Logger.v("TasksOAuthClient") { "Token response: $body" }

        if (!response.isSuccessful) {
            throw Exception("Token exchange failed: ${response.code} $body")
        }

        val json = Json.parseToJsonElement(body) as JsonObject
        val accessToken = json["access_token"]?.jsonPrimitive?.content
            ?: throw Exception("No access_token in response")
        val idTokenStr = json["id_token"]?.jsonPrimitive?.content
            ?: throw Exception("No id_token in response")

        return OAuthResult(
            accessToken = accessToken,
            idToken = IdToken(idTokenStr),
        )
    }
}
