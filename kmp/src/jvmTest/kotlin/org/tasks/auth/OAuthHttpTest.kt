package org.tasks.auth

import kotlinx.coroutines.runBlocking
import okhttp3.CookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.tasks.http.OkHttpClientFactory

class OAuthHttpTest {
    private val built = mutableListOf<OkHttpClient>()
    private val foreground = mutableListOf<Boolean>()

    private val factory = object : OkHttpClientFactory {
        override suspend fun newClient(
            foreground: Boolean,
            cookieKey: String?,
            block: (OkHttpClient.Builder) -> Unit,
        ): OkHttpClient {
            this@OAuthHttpTest.foreground.add(foreground)
            return OkHttpClient.Builder()
                .addInterceptor { chain ->
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("{}".toResponseBody("application/json".toMediaType()))
                        .build()
                }
                .apply(block)
                .build()
                .also { built.add(it) }
        }
    }

    @Test
    fun buildsNoHttpClientUntilTheFirstRequest() {
        factory.oauthClient(foreground = false)

        assertEquals(emptyList<OkHttpClient>(), built)
    }

    @Test
    fun reusesOneHttpClient() = runBlocking {
        val client = factory.oauthClient(foreground = false)

        client.fetchDiscovery("https://example.com/a")
        client.fetchDiscovery("https://example.com/b")

        assertEquals(1, built.size)
    }

    @Test
    fun backgroundRefreshesStayInTheBackground() = runBlocking {
        factory.oauthClient(foreground = false).fetchDiscovery("https://example.com")

        assertEquals(listOf(false), foreground)
    }

    @Test
    fun signInIsInTheForeground() = runBlocking {
        factory.oauthClient(foreground = true).fetchDiscovery("https://example.com")

        assertEquals(listOf(true), foreground)
    }

    @Test
    fun sendsNoCookies() = runBlocking {
        factory.oauthClient(foreground = false).fetchDiscovery("https://example.com")

        assertSame(CookieJar.NO_COOKIES, built.single().cookieJar)
    }
}
