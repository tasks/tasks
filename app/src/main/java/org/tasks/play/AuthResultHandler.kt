package org.tasks.play

interface AuthResultHandler {
    fun authenticationSuccessful(accountName: String)

    fun authenticationFailed(message: String?)
}