package org.tasks.sync.microsoft

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.browser.AnyBrowserMatcher
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import org.tasks.BuildConfig
import org.tasks.auth.DebugConnectionBuilder
import org.tasks.auth.IdentityProvider
import org.tasks.auth.MicrosoftAuthenticationActivity
import org.tasks.auth.MicrosoftAuthenticationActivity.Companion.EXTRA_SERVICE_DISCOVERY
import javax.inject.Inject

@HiltViewModel
class MicrosoftSignInViewModel @Inject constructor(
    private val debugConnectionBuilder: DebugConnectionBuilder,
) : ViewModel() {
    fun signIn(activity: Activity) {
        viewModelScope.launch {
            val idp = IdentityProvider.MICROSOFT
            val serviceConfig = idp.retrieveConfig()
            val authRequest = AuthorizationRequest
                .Builder(
                    serviceConfig,
                    idp.clientId,
                    ResponseTypeValues.CODE,
                    idp.redirectUri
                )
                .setScope(idp.scope)
                .setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
                .build()
            val intent = Intent(activity, MicrosoftAuthenticationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(
                EXTRA_SERVICE_DISCOVERY,
                serviceConfig.discoveryDoc!!.docJson.toString()
            )

            val authorizationService = AuthorizationService(
                activity,
                AppAuthConfiguration.Builder()
                    .setBrowserMatcher(AnyBrowserMatcher.INSTANCE)
                    .setConnectionBuilder(
                        if (BuildConfig.DEBUG) {
                            debugConnectionBuilder
                        } else {
                            DefaultConnectionBuilder.INSTANCE
                        }
                    )
                    .build()
            )
            authorizationService.performAuthorizationRequest(
                authRequest,
                PendingIntent.getActivity(
                    activity,
                    authRequest.hashCode(),
                    intent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                ),
                authorizationService.createCustomTabsIntentBuilder()
                    .build()
            )
        }
    }
}