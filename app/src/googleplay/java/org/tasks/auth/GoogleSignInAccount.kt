package org.tasks.auth

import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class GoogleSignInAccount(
        private val account: GoogleSignInAccount
) : OauthSignIn {
    override val id: String?
        get() = account.id

    override val idToken: String?
        get() = account.idToken

    override val email: String?
        get() = account.email
}