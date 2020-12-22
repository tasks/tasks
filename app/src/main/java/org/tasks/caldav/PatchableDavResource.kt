package org.tasks.caldav

import at.bitfire.dav4jvm.DavResource
import at.bitfire.dav4jvm.DavResource.Companion.MAX_REDIRECTS
import at.bitfire.dav4jvm.exception.*
import okhttp3.*
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection

class PatchableDavResource(client: OkHttpClient, url: HttpUrl) : DavResource(client, url) {

    /**
     * Sends a PROPPATCH request to this resource. Follows up to [MAX_REDIRECTS] redirects.
     *
     * @throws IOException on I/O error
     * @throws HttpException on HTTP error
     */
    @Throws(IOException::class, HttpException::class)
    fun propPatch(xmlBody: String?, callback: (response: Response) -> Unit) {
        val rqBody = xmlBody?.let { RequestBody.create(MIME_XML, it) }

        followRedirects {
            httpClient.newCall(Request.Builder()
                    .method("PROPPATCH", rqBody)
                    .url(location)
                    .build()).execute()
        }.use { response ->
            checkStatus(response)
            callback(response)
        }
    }

    /**
     * Checks the status from an HTTP response and throws an exception in case of an error.
     *
     * @throws HttpException in case of an HTTP error
     */
    private fun checkStatus(response: Response) =
            checkStatus(response.code, response.message, response)

    /**
     * Checks the status from an HTTP response and throws an exception in case of an error.
     *
     * @throws HttpException (with XML error names, if available) in case of an HTTP error
     */
    private fun checkStatus(code: Int, message: String?, response: Response?) {
        if (code / 100 == 2)
        // everything OK
            return

        throw when (code) {
            HttpURLConnection.HTTP_UNAUTHORIZED ->
                if (response != null) UnauthorizedException(response) else UnauthorizedException(message)
            HttpURLConnection.HTTP_FORBIDDEN ->
                if (response != null) ForbiddenException(response) else ForbiddenException(message)
            HttpURLConnection.HTTP_NOT_FOUND ->
                if (response != null) NotFoundException(response) else NotFoundException(message)
            HttpURLConnection.HTTP_CONFLICT ->
                if (response != null) ConflictException(response) else ConflictException(message)
            HttpURLConnection.HTTP_PRECON_FAILED ->
                if (response != null) PreconditionFailedException(response) else PreconditionFailedException(message)
            HttpURLConnection.HTTP_UNAVAILABLE ->
                if (response != null) ServiceUnavailableException(response) else ServiceUnavailableException(message)
            else ->
                if (response != null) HttpException(response) else HttpException(code, message)
        }
    }
}