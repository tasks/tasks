package org.tasks

import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.network.NetworkReporter
import com.facebook.flipper.plugins.network.NetworkReporter.ResponseInfo
import com.google.api.client.http.*
import com.google.api.client.json.GenericJson
import org.tasks.data.UUIDHelper
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException

internal class FlipperHttpInterceptor<T>(private val plugin: NetworkFlipperPlugin, private val responseClass: Class<T>) : HttpExecuteInterceptor, HttpResponseInterceptor {
    private val requestId = UUIDHelper.newUUID()

    var response: T? = null
        private set

    override fun intercept(request: HttpRequest) {
        plugin.reportRequest(toRequestInfo(request, currentTimeMillis()))
    }

    @Throws(IOException::class)
    override fun interceptResponse(response: HttpResponse) {
        plugin.reportResponse(toResponseInfo(response, currentTimeMillis()))
    }

    @Throws(IOException::class)
    fun report(response: HttpResponse, start: Long, end: Long) {
        plugin.reportRequest(toRequestInfo(response.request, start))
        plugin.reportResponse(toResponseInfo(response, end))
    }

    private fun toRequestInfo(request: HttpRequest, timestamp: Long): NetworkReporter.RequestInfo {
        val requestInfo = NetworkReporter.RequestInfo()
        requestInfo.method = request.requestMethod
        requestInfo.body = bodyToByteArray(request.content)
        requestInfo.headers = getHeaders(request.headers)
        requestInfo.requestId = requestId
        requestInfo.timeStamp = timestamp
        requestInfo.uri = request.url.toString()
        return requestInfo
    }

    @Throws(IOException::class)
    private fun toResponseInfo(response: HttpResponse, timestamp: Long): ResponseInfo {
        val responseInfo = ResponseInfo()
        responseInfo.timeStamp = timestamp
        responseInfo.headers = getHeaders(response.headers)
        responseInfo.requestId = requestId
        responseInfo.statusCode = response.statusCode
        responseInfo.statusReason = response.statusMessage
        this.response = response.parseAs(responseClass)
        if (this.response is GenericJson) {
            try {
                responseInfo.body = (this.response as GenericJson).toPrettyString().toByteArray()
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
        return responseInfo
    }

    private fun getHeaders(headers: HttpHeaders): List<NetworkReporter.Header> {
        return headers.map { (name, value) -> NetworkReporter.Header(name, value.toString()) }
    }

    private fun bodyToByteArray(content: HttpContent?): ByteArray? {
        if (content == null) {
            return null
        }
        val output = ByteArrayOutputStream()
        try {
            content.writeTo(output)
        } catch (e: IOException) {
            Timber.e(e)
            return null
        }
        return output.toByteArray()
    }
}