package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.tasks.auth.OAuthProvider
import org.tasks.auth.SignInHandler
import org.tasks.compose.accounts.Platform
import kotlin.coroutines.cancellation.CancellationException

class AddAccountViewModel(
    private val signInHandler: SignInHandler,
) : ViewModel() {

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
