package org.tasks.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.tasks.extensions.keyWindow
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.darwin.NSObject
import kotlin.coroutines.cancellation.CancellationException

class AppleCredential(
    val user: String,
    val identityToken: String,
    val authorizationCode: String,
    val email: String?,
    val fullName: String?,
)

@OptIn(ExperimentalForeignApi::class)
class AppleSignIn {
    private var inFlight: Delegate? = null

    suspend fun authorize(nonceHash: String): AppleCredential = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val request = ASAuthorizationAppleIDProvider().createRequest().apply {
                requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
                nonce = nonceHash
            }
            val controller = ASAuthorizationController(authorizationRequests = listOf(request))
            val delegate = Delegate { result ->
                inFlight = null
                if (continuation.isActive) {
                    continuation.resumeWith(result)
                }
            }
            inFlight = delegate
            controller.delegate = delegate
            controller.presentationContextProvider = delegate
            continuation.invokeOnCancellation { inFlight = null }
            controller.performRequests()
        }
    }

    private class Delegate(
        private val onResult: (Result<AppleCredential>) -> Unit,
    ) : NSObject(),
        ASAuthorizationControllerDelegateProtocol,
        ASAuthorizationControllerPresentationContextProvidingProtocol {

        override fun authorizationController(
            controller: ASAuthorizationController,
            didCompleteWithAuthorization: ASAuthorization,
        ) {
            val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
            if (credential == null) {
                onResult(Result.failure(Exception("Unexpected credential type")))
                return
            }
            val identityToken = credential.identityToken?.utf8()
            val authorizationCode = credential.authorizationCode?.utf8()
            if (identityToken == null || authorizationCode == null) {
                onResult(Result.failure(Exception("Apple did not return an identity token")))
                return
            }
            val fullName = credential.fullName?.let { name ->
                listOfNotNull(name.givenName, name.familyName).joinToString(" ").ifBlank { null }
            }
            onResult(
                Result.success(
                    AppleCredential(
                        user = credential.user,
                        identityToken = identityToken,
                        authorizationCode = authorizationCode,
                        email = credential.email,
                        fullName = fullName,
                    )
                )
            )
        }

        override fun authorizationController(
            controller: ASAuthorizationController,
            didCompleteWithError: NSError,
        ) {
            val error = didCompleteWithError
            onResult(
                Result.failure(
                    if (error.code == ASAuthorizationErrorCanceled) {
                        CancellationException("Sign in cancelled")
                    } else {
                        Exception(error.localizedDescription)
                    }
                )
            )
        }

        override fun presentationAnchorForAuthorizationController(
            controller: ASAuthorizationController,
        ): ASPresentationAnchor = keyWindow()

        private fun NSData.utf8(): String? = NSString.create(data = this, encoding = NSUTF8StringEncoding) as String?
    }
}
