package org.tasks.compose.accounts

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.tasks.auth.OAuthProvider
import org.tasks.auth.SignInHandler
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.jobs.BackgroundWork
import org.tasks.sync.SyncAdapters
import org.tasks.viewmodel.AddAccountViewModel as BaseAddAccountViewModel
import kotlin.coroutines.cancellation.CancellationException

class AddAccountViewModel(
    caldavDao: CaldavDao,
    purchaseState: PurchaseState,
    syncAdapters: SyncAdapters,
    backgroundWork: BackgroundWork,
    private val signInHandler: SignInHandler,
) : BaseAddAccountViewModel(caldavDao, purchaseState, syncAdapters, backgroundWork) {

    sealed interface SignInState {
        data object Idle : SignInState
        data object Loading : SignInState
        data class Error(val message: String) : SignInState
    }

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState

    fun signIn(
        platform: Platform,
        provider: OAuthProvider? = null,
        openUrl: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            _signInState.value = SignInState.Loading
            try {
                signInHandler.signIn(platform, provider, openUrl)
                _signInState.value = SignInState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _signInState.value = SignInState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun dismissError() {
        _signInState.value = SignInState.Idle
    }
}
