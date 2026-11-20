package org.tasks.compose.accounts

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.tasks.auth.OAuthProvider
import org.tasks.auth.PaymentRequiredException
import org.tasks.auth.WrongAccountException
import org.tasks.auth.signInError
import org.tasks.billing.PurchaseState
import org.tasks.caldav.PurchaseTokenInUseException
import org.tasks.http.HttpException
import org.tasks.ui.DisplayableException
import kotlin.coroutines.cancellation.CancellationException

sealed interface SignInState {
    data object Idle : SignInState
    data object Loading : SignInState
    data object Finishing : SignInState

    sealed interface Error : SignInState {
        val report: String

        data class Failed(
            val message: String,
            override val report: String = "failed",
        ) : Error

        data object PaymentRequired : Error {
            override val report: String get() = "payment_required"
        }

        data object UpgradeRequired : Error {
            override val report: String get() = "insufficient_subscription"
        }

        data class AccountInUse(val existingAccount: String) : Error {
            override val report: String get() = "purchase_token_in_use"
        }

        data class DifferentAccount(
            val existingAccount: String?,
            val provider: OAuthProvider,
        ) : Error {
            override val report: String get() = "different_account"
        }
    }
}

internal class SignInAttempts(
    private val scope: CoroutineScope,
    private val purchaseState: PurchaseState,
) {
    private val _state = MutableStateFlow<SignInState>(SignInState.Idle)
    val state: StateFlow<SignInState> = _state
    private var current: Job? = null
    private var last: (suspend (onAuthorized: () -> Unit) -> Unit)? = null

    fun start(signIn: suspend (onAuthorized: () -> Unit) -> Unit) {
        if (_state.value == SignInState.Finishing) return
        last = signIn
        current?.cancel()
        _state.value = SignInState.Loading
        val attempt = scope.launch(start = CoroutineStart.LAZY) {
            val self = coroutineContext.job
            val outcome = try {
                signIn {
                    if (current === self) {
                        _state.value = SignInState.Finishing
                    }
                }
                SignInState.Idle
            } catch (e: CancellationException) {
                if (current === self) {
                        _state.value = SignInState.Idle
                }
                throw e
            } catch (e: PurchaseTokenInUseException) {
                SignInState.Error.AccountInUse(e.existingAccount)
            } catch (e: WrongAccountException) {
                SignInState.Error.DifferentAccount(e.existingAccount, e.provider)
            } catch (e: PaymentRequiredException) {
                if (purchaseState.hasPro && !purchaseState.hasTasksSubscription) {
                    SignInState.Error.UpgradeRequired
                } else {
                    SignInState.Error.PaymentRequired
                }
            } catch (e: Exception) {
                Logger.e { "Sign in failed: ${scrubEmails(e.stackTraceToString())}" }
                SignInState.Error.Failed(message = e.signInError(), report = e.report())
            }
            if (current === self) {
                _state.value = outcome
            }
        }
        current = attempt
        attempt.start()
    }

    fun cancel() {
        if (_state.value == SignInState.Finishing) return
        current?.cancel()
        current = null
        if (_state.value == SignInState.Loading) {
            _state.value = SignInState.Idle
        }
    }

    fun retry() {
        if (_state.value !is SignInState.Error) return
        last?.let(::start)
    }

    fun dismissError() {
        if (_state.value is SignInState.Error) {
            _state.value = SignInState.Idle
        }
    }

    private fun Exception.report(): String = when (this) {
        is HttpException -> "http_$code"
        is DisplayableException -> resource.key
        else -> message?.let(::scrubEmails) ?: this::class.simpleName ?: "no_message"
    }
}

private val EMAIL = Regex("[A-Za-z0-9._%+-]+(?:@|%40)[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+")

internal fun scrubEmails(text: String): String = EMAIL.replace(text, "<email>")
