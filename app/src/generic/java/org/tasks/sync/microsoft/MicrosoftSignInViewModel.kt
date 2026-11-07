package org.tasks.sync.microsoft

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.tasks.analytics.Firebase
import org.tasks.auth.TasksOAuthSignIn
import org.tasks.extensions.Context.toast
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MicrosoftSignInViewModel @Inject constructor(
    private val signIn: TasksOAuthSignIn,
    private val firebase: Firebase,
) : ViewModel() {
    fun signIn(activity: Activity, username: String? = null) {
        val context = activity.applicationContext
        viewModelScope.launch {
            try {
                signIn.signInToMicrosoft(
                    openUrl = {},
                    analytics = firebase,
                    extraParams = username
                        ?.let { mapOf("login_hint" to it, "prompt" to "login") }
                        ?: emptyMap(),
                )
                Timber.d("Successfully signed in")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                context.toast(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
