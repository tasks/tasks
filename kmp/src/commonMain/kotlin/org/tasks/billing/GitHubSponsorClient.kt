package org.tasks.billing

interface GitHubSponsorClient {
    sealed interface VerifyResult {
        data object Success : VerifyResult
        data object NotSponsor : VerifyResult
        data object Failed : VerifyResult
    }
    suspend fun signIn(openUrl: (String) -> Unit): VerifyResult
}
