package org.tasks.sync.microsoft

import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import com.microsoft.identity.common.java.exception.ErrorStrings
import org.tasks.http.ConnectionException
import org.tasks.http.UnauthorizedException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun MsalException.toTokenException(): Exception = when {
    this is MsalUiRequiredException -> UnauthorizedException(message, this)
    isNetworkFailure() -> ConnectionException(message, this)
    else -> this
}

private fun MsalException.isNetworkFailure() =
    (errorCode == MsalClientException.IO_ERROR && hasConnectivityCause()) ||
            errorCode == MsalClientException.DEVICE_NETWORK_NOT_AVAILABLE ||
            errorCode == ErrorStrings.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION ||
            errorCode == MsalClientException.BROKER_BIND_FAILURE ||
            message?.contains(NO_ADDRESS_MESSAGE) == true

private fun Throwable.hasConnectivityCause() =
    generateSequence(cause) { it.cause }.any {
        it is UnknownHostException ||
                it is SocketTimeoutException ||
                it is ConnectException ||
                it is NoRouteToHostException
    }

private const val NO_ADDRESS_MESSAGE = "No address associated with hostname"
