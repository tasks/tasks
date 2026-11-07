package org.tasks.billing

interface GitHubSponsorClient {
    sealed interface VerifyResult {
        data object Success : VerifyResult
        data object NotSponsor : VerifyResult
        data class NotStored(val encryptionFailed: Boolean) : VerifyResult
        data object Failed : VerifyResult
        data object Cancelled : VerifyResult
    }
    suspend fun signIn(openUrl: (String) -> Unit): VerifyResult
}
