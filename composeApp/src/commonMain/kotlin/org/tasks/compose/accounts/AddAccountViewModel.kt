package org.tasks.compose.accounts

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.tasks.auth.OAuthProvider
import org.tasks.auth.PaymentRequiredException
import org.tasks.auth.SignInHandler
import org.tasks.caldav.PurchaseTokenInUseException
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao
import org.tasks.viewmodel.AddAccountViewModel as BaseAddAccountViewModel
import kotlin.coroutines.cancellation.CancellationException

class AddAccountViewModel(
    caldavDao: CaldavDao,
    purchaseState: PurchaseState,
    private val signInHandler: SignInHandler,
) : BaseAddAccountViewModel(caldavDao, purchaseState) {

    sealed interface SignInState {
        data object Idle : SignInState
        data object Loading : SignInState

        sealed interface Error : SignInState {
            val report: String

            data class Failed(val message: String) : Error {
                override val report: String get() = message
            }

            data object PaymentRequired : Error {
                override val report: String get() = "payment_required"
            }

            data object UpgradeRequired : Error {
                override val report: String get() = "insufficient_subscription"
            }

            data class AccountInUse(val existingAccount: String) : Error {
                override val report: String get() = "purchase_token_in_use"
            }
        }
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
            } catch (e: PurchaseTokenInUseException) {
                _signInState.value = SignInState.Error.AccountInUse(e.existingAccount)
            } catch (e: PaymentRequiredException) {
                _signInState.value = if (hasPro && !hasTasksSubscription) {
                    SignInState.Error.UpgradeRequired
                } else {
                    SignInState.Error.PaymentRequired
                }
            } catch (e: Exception) {
                _signInState.value = SignInState.Error.Failed(e.message ?: "Sign in failed")
            }
        }
    }

    fun dismissError() {
        _signInState.value = SignInState.Idle
    }
}
