package org.tasks

import android.content.Context
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import java.io.IOException
import javax.inject.Inject

class DebugNetworkInterceptor @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun apply(builder: OkHttpClient.Builder?) {
        builder?.addNetworkInterceptor(FlipperOkhttpInterceptor(getNetworkPlugin(context)))
    }

    @Throws(IOException::class)
    fun <T> execute(request: HttpRequest, responseClass: Class<T>): T? {
        val interceptor = FlipperHttpInterceptor(getNetworkPlugin(context), responseClass)
        request
                .setInterceptor(interceptor)
                .setResponseInterceptor(interceptor)
                .execute()
        return interceptor.response
    }

    @Throws(IOException::class)
    fun <T> report(httpResponse: HttpResponse, responseClass: Class<T>, start: Long, finish: Long): T? {
        val interceptor = FlipperHttpInterceptor(getNetworkPlugin(context), responseClass)
        interceptor.report(httpResponse, start, finish)
        return interceptor.response
    }

    private fun getNetworkPlugin(context: Context): NetworkFlipperPlugin {
        return AndroidFlipperClient.getInstance(context).getPlugin(NetworkFlipperPlugin.ID)!!
    }
}