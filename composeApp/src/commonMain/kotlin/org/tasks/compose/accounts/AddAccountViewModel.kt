package org.tasks.compose.accounts

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import org.tasks.auth.OAuthProvider
import org.tasks.auth.SignInHandler
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.viewmodel.AddAccountViewModel as BaseAddAccountViewModel

class AddAccountViewModel(
    caldavDao: CaldavDao,
    purchaseState: PurchaseState,
    private val signInHandler: SignInHandler,
) : BaseAddAccountViewModel(caldavDao, purchaseState) {

    private val attempts = SignInAttempts(viewModelScope, purchaseState)
    val signInState: StateFlow<SignInState> = attempts.state

    fun signIn(
        platform: Platform,
        provider: OAuthProvider? = null,
    ) {
        attempts.start { onAuthorized ->
            signInHandler.signIn(platform, provider, onAuthorized = onAuthorized)
        }
    }

    fun dismissError() = attempts.dismissError()
}
