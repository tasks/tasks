package org.tasks.auth

interface OauthSignIn {
    val idToken: String?
    val email: String?
    val id: String?
}