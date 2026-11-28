package org.tasks.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosOAuthAuthorizationTest {

    private val serverEnvironment = TasksServerEnvironment(TasksPreferences(InMemoryDataStore()))
    private val requests = mutableListOf<HttpRequestData>()
    private var authUrl: Url? = null
    private var callbackScheme: String? = null

    private val oauthClient = TasksOAuthClient(
        HttpClient(MockEngine { request ->
            requests += request
            val body = if (request.url.encodedPath.endsWith("token")) {
                """{"access_token":"access","refresh_token":"refresh","expires_in":3600}"""
            } else {
                """{
                    "authorization_endpoint": "https://idp.example/authorize",
                    "token_endpoint": "https://idp.example/token"
                }"""
            }
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        })
    )

    private fun authorizer(
        callback: (Url) -> String = { url -> "${url.parameters["redirect_uri"]}?code=c0de&state=${url.parameters["state"]}" },
    ) = OAuthAuthorizer(
        oauthClient = oauthClient,
        serverEnvironment = serverEnvironment,
        redirect = AuthSessionRedirect { url, scheme ->
            authUrl = Url(url)
            callbackScheme = scheme
            callback(Url(url))
        },
    )

    private val discovery get() = requests.first().url.toString()
    private val tokenForm get() = (requests.last().body as FormDataContent).formData

    @Test
    fun googleTasksDiscoversFromGoogleWithTheIosClient() = runTest {
        authorizer().authorize(OAuthProvider.GOOGLE_TASKS, openUrl = {})

        val redirect = OAuthProvider.GOOGLE_TASKS.ios.redirectUri
        assertEquals("https://accounts.google.com/.well-known/openid-configuration", discovery)
        assertEquals(OAuthProvider.GOOGLE_TASKS.ios.clientId, authUrl!!.parameters["client_id"])
        assertEquals(redirect, authUrl!!.parameters["redirect_uri"])
        assertEquals(redirect!!.substringBefore(':'), callbackScheme)
        assertEquals("offline", authUrl!!.parameters["access_type"])
        assertEquals(redirect, tokenForm["redirect_uri"])
    }

    @Test
    fun tasksOrgSignInDiscoversFromTheServer() = runTest {
        authorizer().authorize(OAuthProvider.GITHUB, openUrl = {}, authHeader = "Bearer jwt")

        assertEquals("${serverEnvironment.caldavUrl}/oauth/github-configuration", discovery)
        assertEquals("Bearer jwt", requests.first().headers[HttpHeaders.Authorization])
        assertEquals(OAuthProvider.GITHUB.ios.clientId, authUrl!!.parameters["client_id"])
        assertEquals(OAuthProvider.GITHUB.ios.redirectUri, authUrl!!.parameters["redirect_uri"])
        assertEquals("org.tasks.github.a50fdbf3e289a7fb2fc6", callbackScheme)
    }

    @Test
    fun microsoftUsesTheIosBrokerRedirect() = runTest {
        authorizer().authorize(OAuthProvider.MICROSOFT, openUrl = {})

        assertEquals("https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration", discovery)
        assertEquals("msauth.org.tasks://auth", authUrl!!.parameters["redirect_uri"])
        assertEquals("msauth.org.tasks", callbackScheme)
    }

    @Test
    fun theCodeIsExchangedWithPkce() = runTest {
        val result = authorizer().authorize(OAuthProvider.GOOGLE, openUrl = {})

        assertEquals("access", result.accessToken)
        assertEquals("https://idp.example/token", requests.last().url.toString())
        assertEquals("c0de", tokenForm["code"])
        assertEquals("S256", authUrl!!.parameters["code_challenge_method"])
        assertNotNull(tokenForm["code_verifier"])
    }

    @Test
    fun aMismatchedStateIsNeverExchanged() = runTest {
        val error = assertFailsWith<IOException> {
            authorizer { url -> "${url.parameters["redirect_uri"]}?code=c0de&state=other" }
                .authorize(OAuthProvider.GOOGLE, openUrl = {})
        }

        assertEquals("OAuth state mismatch", error.message)
        assertNotNull(authUrl)
        assertTrue(requests.none { it.url.encodedPath.endsWith("token") })
    }

    @Test
    fun closingTheBrowserCancelsTheSignIn() = runTest {
        assertFailsWith<CancellationException> {
            authorizer { throw CancellationException("Sign in cancelled") }
                .authorize(OAuthProvider.GOOGLE, openUrl = {})
        }
    }
}
