package org.tasks.sync.microsoft

import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import org.tasks.http.ConnectionException
import org.tasks.http.UnauthorizedException
import java.io.IOException

internal fun MsalException.toTokenException(): Exception = when {
    this is MsalUiRequiredException -> UnauthorizedException(message, this)
    isNetworkFailure() -> ConnectionException(message, this)
    else -> this
}

private fun MsalException.isNetworkFailure() =
    errorCode == MsalClientException.IO_ERROR ||
            errorCode == MsalClientException.DEVICE_NETWORK_NOT_AVAILABLE ||
            generateSequence(cause) { it.cause }.any { it is IOException }
