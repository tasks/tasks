package org.tasks.googleapis

import co.touchlab.kermit.Logger
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest
import com.google.api.client.http.HttpResponseException
import com.google.api.client.json.GenericJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.TasksBuildConfig
import java.io.IOException

abstract class BaseInvoker(
    private val credentialsAdapter: CredentialsAdapter,
) {
    @Throws(IOException::class)
    protected suspend fun <T> execute(request: AbstractGoogleJsonClientRequest<T>): T? =
        execute(request, false)

    @Throws(IOException::class)
    private suspend fun <T> execute(
        request: AbstractGoogleJsonClientRequest<T>,
        retry: Boolean,
    ): T? = withContext(Dispatchers.IO) {
        credentialsAdapter.checkToken()
        val desc = describeRequest(request)
        Logger.d(TAG) { desc }
        val response: T? = try {
            request.execute()
        } catch (e: HttpResponseException) {
            return@withContext if (e.statusCode == 401 && !retry) {
                credentialsAdapter.invalidateToken()
                execute(request, true)
            } else if (e.statusCode == 404) {
                throw HttpNotFoundException(e)
            } else {
                throw e
            }
        }
        Logger.d(TAG) {
            "$desc -> ${if (TasksBuildConfig.DEBUG) prettyPrint(response) else "<redacted>"}"
        }
        response
    }

    private fun <T> prettyPrint(`object`: T?): Any? {
        if (TasksBuildConfig.DEBUG) {
            if (`object` is GenericJson) {
                return (`object` as GenericJson).toPrettyString()
            }
        }
        return `object`
    }

    private fun <T> describeRequest(request: AbstractGoogleJsonClientRequest<T>): String =
        try {
            "${request.requestMethod} ${request.buildHttpRequestUrl()}"
        } catch (_: Exception) { "" }

    companion object {
        private const val TAG = "BaseInvoker"
        val APP_NAME = "Tasks/${TasksBuildConfig.VERSION_NAME}"
    }
}
