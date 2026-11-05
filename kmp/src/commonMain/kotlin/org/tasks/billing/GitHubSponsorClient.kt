package org.tasks.billing

interface GitHubSponsorClient {
    sealed interface VerifyResult {
        data object Success : VerifyResult
        data object NotSponsor : VerifyResult
        data class NotStored(val encryptionFailed: Boolean) : VerifyResult
        data object Failed : VerifyResult
    }
    suspend fun signIn(): VerifyResult
}
