package org.tasks.auth

import android.net.Uri
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.tasks.auth.SignInViewModel.Companion.createCodeTokenRequest

@RunWith(RobolectricTestRunner::class)
class CodeTokenRequestTest {
    @Test
    fun theTokenRequestExpectsTheNonceTheAuthorizationRequestSent() {
        val response = response()

        assertEquals(response.request.nonce, response.createCodeTokenRequest().nonce)
    }

    @Test
    fun aServerVerifierIsSentInPlaceOfTheRequestsOwn() {
        val request = response().createCodeTokenRequest(codeVerifier = VERIFIER)

        assertEquals(VERIFIER, request.codeVerifier)
    }

    private fun response(): AuthorizationResponse {
        val request = AuthorizationRequest.Builder(
            AuthorizationServiceConfiguration(
                Uri.parse("https://appleid.apple.com/auth/authorize"),
                Uri.parse("https://caldav.tasks.org/apple_token"),
            ),
            "org.tasks.signin",
            ResponseTypeValues.CODE,
            Uri.parse("https://caldav.tasks.org/oauth/apple/callback"),
        )
            .setNonce("n0nce-from-the-request")
            .setCodeVerifier(null)
            .build()
        return AuthorizationResponse.Builder(request)
            .setAuthorizationCode("c0de")
            .build()
    }

    private companion object {
        const val VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
    }
}
