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
import androidx.activity.viewModels
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import at.bitfire.dav4jvm.exception.HttpException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.openid.appauth.*
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseDialog
import org.tasks.billing.PurchaseDialog.Companion.FRAG_TAG_PURCHASE_DIALOG
import org.tasks.billing.PurchaseDialog.Companion.newPurchaseDialog
import org.tasks.injection.InjectingAppCompatActivity
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
class SignInActivity : InjectingAppCompatActivity(), PurchaseDialog.PurchaseHandler {
    @Inject lateinit var authorizationServiceProvider: AuthorizationServiceProvider
    @Inject lateinit var authStateManager: AuthStateManager
    @Inject lateinit var themeColor: ThemeColor
    @Inject lateinit var inventory: Inventory

    private val viewModel: SignInViewModel by viewModels()

    private val mClientId = AtomicReference<String?>()
    private val mAuthRequest = AtomicReference<AuthorizationRequest>()
    private val mAuthIntent = AtomicReference<CustomTabsIntent>()
    private var mAuthIntentLatch = CountDownLatch(1)
    private val mExecutor: ExecutorService = newSingleThreadExecutor()

    lateinit var authService: AuthorizationService
    lateinit var configuration: Configuration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.error.observe(this, this::handleError)

        authService = authorizationServiceProvider.google
        configuration = authService.configuration

        if (authStateManager.current.isAuthorized &&
                !configuration.hasConfigurationChanged()) {
            Timber.i("User is already authenticated, signing out")
            authStateManager.signOut()
        }
        if (!configuration.isValid) {
            returnError(configuration.configurationError)
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
            newPurchaseDialog(tasksPayment = true)
                    .show(supportFragmentManager, FRAG_TAG_PURCHASE_DIALOG)
        } else {
            returnError(e.message)
        }
    }

    override fun onStop() {
        super.onStop()

        mExecutor.shutdownNow()
    }

    override fun onDestroy() {
        super.onDestroy()

        authService.dispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_AUTH) {
            if (resultCode == RESULT_OK) {
                lifecycleScope.launch {
                    val account = try {
                        viewModel.handleResult(data!!)
                    } catch (e: Exception) {
                        returnError(e.message)
                    }
                    if (account != null) {
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            } else {
                returnError(getString(R.string.authorization_cancelled))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
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
            Timber.i(ex, "Failed to retrieve discovery document")
            returnError("Failed to retrieve discovery document: " + ex!!.message)
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
            Timber.i(ex, "Failed to dynamically register client")
            displayErrorLater("Failed to register client: " + ex!!.message)
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
    private fun returnError(error: String?) {
        Timber.e(error)
        setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, error))
        finish()
    }

    // WrongThread inference is incorrect in this case
    @AnyThread
    private fun displayErrorLater(error: String) {
        runOnUiThread { returnError(error) }
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
        private const val RC_AUTH = 100
    }

    override fun onPurchaseDialogDismissed() {
        if (inventory.subscription?.isTasksSubscription == true) {
            lifecycleScope.launch {
                val account = viewModel.setupAccount(authStateManager.current)
                if (account != null) {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        } else {
            finish()
        }
    }
}