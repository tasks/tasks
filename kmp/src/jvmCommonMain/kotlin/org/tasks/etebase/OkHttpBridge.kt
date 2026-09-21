package org.tasks.etebase

import com.etebase.client.Client
import com.etebase.client.HttpClient
import com.etebase.client.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OkHttpBridge(private val client: OkHttpClient) : HttpClient {

    override fun get(url: String, token: String?, response: Response) {
        execute(response) {
            Request.Builder()
                .url(url.toHttpUrl())
                .header(ACCEPT, MSGPACK)
                .apply { token?.let { header(AUTHORIZATION, "Token $it") } }
                .get()
                .build()
        }
    }

    override fun post(url: String, token: String?, body: ByteArray, response: Response) {
        execute(response) {
            Request.Builder()
                .url(url.toHttpUrl())
                .header(ACCEPT, MSGPACK)
                .apply { token?.let { header(AUTHORIZATION, "Token $it") } }
                .post(body.toRequestBody(MSGPACK_TYPE))
                .build()
        }
    }

    override fun put(url: String, token: String?, body: ByteArray, response: Response) {
        execute(response) {
            Request.Builder()
                .url(url.toHttpUrl())
                .header(ACCEPT, MSGPACK)
                .apply { token?.let { header(AUTHORIZATION, "Token $it") } }
                .put(body.toRequestBody(MSGPACK_TYPE))
                .build()
        }
    }

    override fun patch(url: String, token: String?, body: ByteArray, response: Response) {
        execute(response) {
            Request.Builder()
                .url(url.toHttpUrl())
                .header(ACCEPT, MSGPACK)
                .apply { token?.let { header(AUTHORIZATION, "Token $it") } }
                .patch(body.toRequestBody(MSGPACK_TYPE))
                .build()
        }
    }

    override fun del(url: String, token: String?, response: Response) {
        execute(response) {
            Request.Builder()
                .url(url.toHttpUrl())
                .header(ACCEPT, MSGPACK)
                .apply { token?.let { header(AUTHORIZATION, "Token $it") } }
                .delete()
                .build()
        }
    }

    private fun execute(response: Response, requestBuilder: () -> Request) {
        try {
            val result = client.newCall(requestBuilder()).execute()
            response.reset_ok(result.body!!.bytes(), result.code)
        } catch (e: Exception) {
            response.reset_err(e.toString())
        }
    }

    companion object {
        private const val ACCEPT = "ACCEPT"
        private const val AUTHORIZATION = "Authorization"
        private const val MSGPACK = "application/msgpack"
        private val MSGPACK_TYPE = MSGPACK.toMediaType()

        fun createClient(httpClient: OkHttpClient, serverUrl: String): Client =
            Client.client_new_with_impl(serverUrl, OkHttpBridge(httpClient))
    }
}
