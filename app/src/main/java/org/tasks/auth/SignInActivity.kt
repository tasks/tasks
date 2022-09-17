/*
 * Copyright 2015 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tasks.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import at.bitfire.dav4jvm.exception.HttpException
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.openid.appauth.*
import org.tasks.R
import org.tasks.Tasks.Companion.IS_GENERIC
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivity.Companion.EXTRA_GITHUB
import org.tasks.compose.ConsentDialog
import org.tasks.compose.SignInDialog
import org.tasks.dialogs.DialogBuilder
import org.tasks.extensions.Context.openUri
import org.tasks.themes.ThemeColor
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * Demonstrates the usage of the AppAuth to authorize a user with an OAuth2 / OpenID Connect
 * provider
 *
 * - Retrieve an OpenID Connect discovery document for the provider, or use a local static
 * configuration.
 * - Utilize dynamic client registration, if no static client id is specified.
 * - Initiate the authorization request using the built-in heuristics or a user-selected browser.
 */
@AndroidEntryPoint
class SignInActivity : ComponentActivity() {
    @Inject lateinit var themeColor: ThemeColor
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var firebase: Firebase

    private val viewModel: SignInViewModel by viewModels()

    private val mClientId = AtomicReference<String?>()
    private val mAuthRequest = AtomicReference<AuthorizationRequest>()
    private val mAuthIntent = AtomicReference<CustomTabsIntent>()
    private var mAuthIntentLatch = CountDownLatch(1)
    private val mExecutor: ExecutorService = newSingleThreadExecutor()

    private val authService: AuthorizationService
        get() = viewModel.authService!!

    private val configuration: Configuration
        get() = authService.configuration

    private val authStateManager: AuthStateManager
        get() = authService.authStateManager

    enum class Platform {
        GOOGLE,
        GITHUB,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.error.observe(this, this::handleError)

        val autoSelect = intent.getSerializableExtra(EXTRA_SELECT_SERVICE) as Platform?
        setContent {
            var selectedPlatform by rememberSaveable {
                mutableStateOf(autoSelect)
            }
            MdcTheme {
                selectedPlatform
                    ?.let {
                        Dialog(onDismissRequest = { finish() }) {
                            ConsentDialog { agree ->
                                if (agree) {
                                    selectService(it)
                                } else {
                                    finish()
                                }
                            }
                        }
                    }
                    ?: Dialog(onDismissRequest = { finish() }) {
                        SignInDialog(
                            selected = { selectedPlatform = it },
                            help = {
                                openUri(R.string.help_url_sync)
                                finish()
                            },
                            cancel = { finish() }
                        )
                    }
            }
        }
    }

    private fun selectService(which: Platform) {
        viewModel.initializeAuthService(when (which) {
            Platform.GOOGLE -> AuthorizationService.ISS_GOOGLE
            Platform.GITHUB -> AuthorizationService.ISS_GITHUB
        })
        startAuthorization()
    }

    private fun startAuthorization() {
        if (authStateManager.current.isAuthorized &&
                !configuration.hasConfigurationChanged()) {
            Timber.i("User is already authenticated, signing out")
            authStateManager.signOut()
        }
        if (!configuration.isValid) {
            returnError(Exception(configuration.configurationError))
            return
        }
        if (configuration.hasConfigurationChanged()) {
            // discard any existing authorization state due to the change of configuration
            Timber.i("Configuration change detected, discarding old state")
            authStateManager.replace(AuthState())
            configuration.acceptConfiguration()
        }
        mExecutor.submit { initializeAppAuth() }
    }

    private fun handleError(e: Throwable) {
        if (e is HttpException && e.code == 402) {
            startActivityForResult(
                Intent(this, PurchaseActivity::class.java)
                    .putExtra(EXTRA_GITHUB, viewModel.authService?.isGitHub ?: IS_GENERIC),
                RC_PURCHASE
            )
        } else {
            returnError(e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mExecutor.shutdownNow()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_PURCHASE ->
                if (inventory.subscription.value?.isTasksSubscription == true) {
                    lifecycleScope.launch {
                        val account = viewModel.setupAccount(authService)
                        if (account != null) {
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                } else {
                    finish()
                }
            RC_AUTH ->
                if (resultCode == RESULT_OK) {
                    lifecycleScope.launch {
                        val account = try {
                            viewModel.handleResult(authService, data!!)
                        } catch (e: Exception) {
                            returnError(e)
                        }
                        if (account != null) {
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                } else {
                    returnError(
                        Exception(getString(R.string.authorization_cancelled)),
                        report = false
                    )
                }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @MainThread
    fun startAuth() {
        // WrongThread inference is incorrect for lambdas
        // noinspection WrongThread
        mExecutor.submit { doAuth() }
    }

    /**
     * Initializes the authorization service configuration if necessary, either from the local
     * static values or by retrieving an OpenID discovery document.
     */
    @WorkerThread
    private fun initializeAppAuth() {
        Timber.i("Initializing AppAuth")
        if (authStateManager.current.authorizationServiceConfiguration != null) {
            // configuration is already created, skip to client initialization
            Timber.i("auth config already established")
            initializeClient()
            return
        }

        // if we are not using discovery, build the authorization service configuration directly
        // from the static configuration values.
        if (configuration.discoveryUri == null) {
            Timber.i("Creating auth config")
            val config = AuthorizationServiceConfiguration(
                    configuration.authEndpointUri!!,
                    configuration.tokenEndpointUri!!,
                    configuration.registrationEndpointUri)
            authStateManager.replace(AuthState(config))
            initializeClient()
            return
        }

        // WrongThread inference is incorrect for lambdas
        // noinspection WrongThread
        Timber.i("Retrieving OpenID discovery doc")
        AuthorizationServiceConfiguration.fetchFromUrl(
                configuration.discoveryUri!!, { config: AuthorizationServiceConfiguration?, ex: AuthorizationException? -> handleConfigurationRetrievalResult(config, ex) },
                configuration.connectionBuilder)
    }

    @MainThread
    private fun handleConfigurationRetrievalResult(
            config: AuthorizationServiceConfiguration?,
            ex: AuthorizationException?) {
        if (config == null) {
            returnError(ex ?: Exception("Failed to retrieve discovery document"))
            return
        }
        Timber.i("Discovery document retrieved")
        authStateManager.replace(AuthState(config))
        mExecutor.submit { initializeClient() }
    }

    /**
     * Initiates a dynamic registration request if a client ID is not provided by the static
     * configuration.
     */
    @WorkerThread
    private fun initializeClient() {
        if (configuration.clientId != null) {
            Timber.i("Using static client ID: %s", configuration.clientId)
            // use a statically configured client ID
            mClientId.set(configuration.clientId)
            runOnUiThread { initializeAuthRequest() }
            return
        }
        val lastResponse = authStateManager.current.lastRegistrationResponse
        if (lastResponse != null) {
            Timber.i("Using dynamic client ID: %s", lastResponse.clientId)
            // already dynamically registered a client ID
            mClientId.set(lastResponse.clientId)
            runOnUiThread { initializeAuthRequest() }
            return
        }

        // WrongThread inference is incorrect for lambdas
        // noinspection WrongThread
        Timber.i("Dynamically registering client")
        val registrationRequest = RegistrationRequest.Builder(
                authStateManager.current.authorizationServiceConfiguration!!, listOf(configuration.redirectUri))
                .setTokenEndpointAuthenticationMethod(ClientSecretBasic.NAME)
                .build()
        authService.performRegistrationRequest(
                registrationRequest) { response: RegistrationResponse?, ex: AuthorizationException? -> handleRegistrationResponse(response, ex) }
    }

    @MainThread
    private fun handleRegistrationResponse(
            response: RegistrationResponse?,
            ex: AuthorizationException?) {
        authStateManager.updateAfterRegistration(response, ex)
        if (response == null) {
            runOnUiThread { returnError(ex ?: Exception("Failed to dynamically register client")) }
            return
        }
        Timber.i("Dynamically registered client: %s", response.clientId)
        mClientId.set(response.clientId)
        initializeAuthRequest()
        return
    }

    /**
     * Performs the authorization request, using the browser selected in the spinner,
     * and a user-provided `login_hint` if available.
     */
    @WorkerThread
    private fun doAuth() {
        try {
            mAuthIntentLatch.await()
        } catch (ex: InterruptedException) {
            Timber.w("Interrupted while waiting for auth intent")
        }
        val intent = authService.getAuthorizationRequestIntent(
                mAuthRequest.get(),
                mAuthIntent.get())
        startActivityForResult(intent, RC_AUTH)
    }

    @MainThread
    private fun returnError(e: Throwable, report: Boolean = true) {
        if (report) {
            firebase.reportException(e)
        }
        setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, e.message))
        finish()
    }

    @MainThread
    private fun initializeAuthRequest() {
        createAuthRequest()
        warmUpBrowser()
        startAuth()
    }

    private fun warmUpBrowser() {
        mAuthIntentLatch = CountDownLatch(1)
        mExecutor.execute {
            Timber.i("Warming up browser instance for auth request")
            mAuthIntent.set(authService.createCustomTabsIntent(
                    mAuthRequest.get().toUri(),
                    themeColor.primaryColor
            ))
            mAuthIntentLatch.countDown()
        }
    }

    private fun createAuthRequest() {
        Timber.i("Creating auth request")
        val authRequestBuilder = AuthorizationRequest.Builder(
                authStateManager.current.authorizationServiceConfiguration!!,
                mClientId.get()!!,
                ResponseTypeValues.CODE,
                configuration.redirectUri)
                .setScope(configuration.scope)
        mAuthRequest.set(authRequestBuilder.build())
    }

    companion object {
        const val EXTRA_ERROR = "extra_error"
        const val EXTRA_SELECT_SERVICE = "extra_select_service"
        private const val RC_AUTH = 100
        private const val RC_PURCHASE = 101
    }
}