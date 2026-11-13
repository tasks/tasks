package org.tasks.auth

fun interface MicrosoftSignIn {
    suspend fun signIn(loginHint: String?)
}
