package org.tasks.googleapis

import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest
import com.google.api.client.http.HttpResponseException
import com.google.api.client.json.GenericJson
import com.todoroo.astrid.gtasks.api.HttpCredentialsAdapter
import com.todoroo.astrid.gtasks.api.HttpNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import org.tasks.DebugNetworkInterceptor
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import java.io.IOException

abstract class BaseInvoker(
        private val credentialsAdapter: HttpCredentialsAdapter,
        private val preferences: Preferences,
        private val interceptor: DebugNetworkInterceptor
) {
    @Throws(IOException::class)
    protected suspend fun <T> execute(request: AbstractGoogleJsonClientRequest<T>): T? = execute(request, false)

    @Throws(IOException::class)
    private suspend fun <T> execute(request: AbstractGoogleJsonClientRequest<T>, retry: Boolean): T? =
            withContext(Dispatchers.IO) {
                credentialsAdapter.checkToken()
                Timber.d("%s request: %s", caller, request)
                val response: T? = try {
                    if (preferences.isFlipperEnabled) {
                        val start = currentTimeMillis()
                        val httpResponse = request.executeUnparsed()
                        interceptor.report(httpResponse, request.responseClass, start,
                            currentTimeMillis()
                        )
                    } else {
                        request.execute()
                    }
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
                Timber.d("%s response: %s", caller, prettyPrint(response))
                response
            }

    @Throws(IOException::class)
    private fun <T> prettyPrint(`object`: T?): Any? {
        if (BuildConfig.DEBUG) {
            if (`object` is GenericJson) {
                return (`object` as GenericJson).toPrettyString()
            }
        }
        return `object`
    }

    private val caller: String
        get() {
            if (BuildConfig.DEBUG) {
                try {
                    return Thread.currentThread().stackTrace[4].methodName
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            return ""
        }

    companion object {
        const val APP_NAME = "Tasks/${BuildConfig.VERSION_NAME}"
    }
}