package org.tasks.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.analytics.Firebase
import org.tasks.caldav.PurchaseTokenInUseException
import org.tasks.http.HttpException
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signIn: TasksOAuthSignIn,
    private val firebase: Firebase,
) : ViewModel() {
    sealed interface State {
        data object Idle : State

        data object Authorizing : State

        data class SubscriptionRequired(
            val provider: OAuthProvider,
            val authorization: OAuthResult,
            val purchasing: Boolean = false,
        ) : State

        data class WrongAccount(val maskedEmail: String) : State

        data class Finished(val outcome: Outcome) : State
    }

    sealed interface Outcome {
        data object Created : Outcome

        data object Cancelled : Outcome

        data object Superseded : Outcome

        data class Failed(val message: String?) : Outcome
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun signIn(provider: OAuthProvider) {
        if (_state.value != State.Idle) {
            return
        }
        launch {
            val authorization = signIn.authorize(provider = provider, openUrl = {})
            createAccount(provider, authorization)
        }
    }

    fun purchaseStarted() {
        _state.update { if (it is State.SubscriptionRequired) it.copy(purchasing = true) else it }
    }

    fun retryAfterPurchase() {
        val required = _state.value as? State.SubscriptionRequired ?: return
        launch { createAccount(required.provider, required.authorization) }
    }

    private suspend fun createAccount(provider: OAuthProvider, authorization: OAuthResult): State =
        try {
            signIn.createAccount(provider, authorization)
            State.Finished(Outcome.Created)
        } catch (e: PaymentRequiredException) {
            State.SubscriptionRequired(provider, authorization)
        }

    private fun launch(block: suspend () -> State) {
        _state.value = State.Authorizing
        viewModelScope.launch {
            _state.value = try {
                block()
            } catch (e: PurchaseTokenInUseException) {
                State.WrongAccount(e.existingAccount)
            } catch (e: RedirectSupersededException) {
                State.Finished(Outcome.Superseded)
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
                State.Finished(Outcome.Cancelled)
            } catch (e: Exception) {
                if (e !is HttpException) {
                    firebase.reportException(e)
                }
                State.Finished(Outcome.Failed(e.signInError()))
            }
        }
    }
}
