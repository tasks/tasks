package org.tasks.compose.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import org.tasks.auth.OAuthProvider
import org.tasks.auth.SignInHandler
import org.tasks.billing.PurchaseState
import org.tasks.compose.accounts.Platform
import org.tasks.compose.accounts.SignInAttempts
import org.tasks.compose.accounts.SignInState
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

class SignInAgainViewModel(
    private val signInHandler: SignInHandler,
    purchaseState: PurchaseState,
    private val syncAdapters: SyncAdapters,
) : ViewModel() {

    private val attempts = SignInAttempts(viewModelScope, purchaseState)
    val signInState: StateFlow<SignInState> = attempts.state

    fun signIn(
        platform: Platform,
        provider: OAuthProvider? = null,
        loginHint: String? = null,
    ) {
        attempts.start { onAuthorized ->
            signInHandler.signIn(platform, provider, loginHint, onAuthorized)
            syncAdapters.sync(SyncSource.USER_INITIATED)
        }
    }

    fun cancel() = attempts.cancel()

    fun dismissError() = attempts.dismissError()
}
