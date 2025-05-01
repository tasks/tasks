package org.tasks.sync.microsoft

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.analytics.Constants
import org.tasks.analytics.Firebase
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.extensions.Context.toast
import org.tasks.jobs.WorkManager
import org.tasks.sync.SyncAdapters
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MicrosoftSignInViewModel @Inject constructor(
    private val caldavDao: CaldavDao,
    private val firebase: Firebase,
    private val syncAdapters: SyncAdapters,
    private val workManager: WorkManager,
) : ViewModel() {
    fun signIn(activity: Activity) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = PublicClientApplication.createMultipleAccountPublicClientApplication(
                activity,
                R.raw.microsoft_config
            )

            val parameters = AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activity)
                .withScopes(scopes)
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .withCallback(object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        val email = authenticationResult.account.claims?.get("preferred_username") as? String
                        if (email == null) {
                            Timber.e("No email found")
                            return
                        }
                        Timber.d("Successfully signed in")
                        viewModelScope.launch {
                            caldavDao
                                .getAccount(TYPE_MICROSOFT, email)
                                ?.let {
                                    caldavDao.update(
                                        it.copy(error = null)
                                    )
                                }
                                ?: caldavDao
                                    .insert(
                                        CaldavAccount(
                                            uuid = UUIDHelper.newUUID(),
                                            name = email,
                                            username = email,
                                            accountType = TYPE_MICROSOFT,
                                        )
                                    )
                                    .also {
                                        firebase.logEvent(
                                            R.string.event_sync_add_account,
                                            R.string.param_type to Constants.SYNC_TYPE_MICROSOFT
                                        )
                                    }
                            syncAdapters.sync(true)
                            workManager.updateBackgroundSync()
                        }
                    }

                    override fun onError(exception: MsalException?) {
                        Timber.e(exception)
                        activity.toast(exception?.message ?: exception?.javaClass?.simpleName ?: "Sign in failed")
                    }

                    override fun onCancel() {
                        Timber.d("onCancel")
                    }
                })
                .build()

            app.acquireToken(parameters)
        }
    }

    companion object {
        val scopes = listOf("https://graph.microsoft.com/.default")
    }
}
