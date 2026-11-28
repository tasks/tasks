package org.tasks.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.tasks.extensions.keyWindow
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodeCanceledLogin
import platform.Foundation.NSError
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private class KeyWindowProvider : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor =
        keyWindow()
}

@OptIn(ExperimentalForeignApi::class)
internal suspend fun authenticate(authUrl: String, callbackScheme: String): String = withContext(Dispatchers.Main) {
    val contextProvider = KeyWindowProvider()
    suspendCancellableCoroutine { continuation ->
        val session = ASWebAuthenticationSession(
            uRL = NSURL.URLWithString(authUrl)!!,
            callbackURLScheme = callbackScheme,
        ) { callbackUrl: NSURL?, error: NSError? ->
            when {
                callbackUrl != null -> continuation.resume(callbackUrl.absoluteString!!)
                error?.code == ASWebAuthenticationSessionErrorCodeCanceledLogin ->
                    continuation.resumeWithException(CancellationException("Sign in cancelled"))
                else -> continuation.resumeWithException(Exception(error?.localizedDescription ?: "Authorization failed"))
            }
        }
        session.presentationContextProvider = contextProvider
        session.prefersEphemeralWebBrowserSession = false
        continuation.invokeOnCancellation {
            if (NSThread.isMainThread) {
                session.cancel()
            } else {
                dispatch_async(dispatch_get_main_queue()) { session.cancel() }
            }
        }
        if (!session.start()) {
            continuation.resumeWithException(Exception("Could not start the sign-in session"))
        }
    }
}
