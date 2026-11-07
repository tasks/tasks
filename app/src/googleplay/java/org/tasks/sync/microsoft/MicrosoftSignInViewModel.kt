package org.tasks.sync.microsoft

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.analytics.Constants
import org.tasks.analytics.Firebase
import org.tasks.data.dao.CaldavDao
import org.tasks.extensions.Context.toast
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MicrosoftSignInViewModel @Inject constructor(
    private val tokenProvider: MicrosoftTokenProvider,
    private val caldavDao: CaldavDao,
    private val firebase: Firebase,
) : ViewModel() {
    fun signIn(activity: Activity, username: String? = null) {
        viewModelScope.launch {
            try {
                val email = tokenProvider.signIn(activity, username)
                Timber.d("Successfully signed in")
                setupMsalMicrosoftAccount(email, caldavDao) {
                    firebase.logEvent(
                        R.string.event_sync_add_account,
                        R.string.param_type to Constants.SYNC_TYPE_MICROSOFT
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                activity.toast(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
