/*
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import okio.Buffer
import okio.buffer
import okio.source
import org.json.JSONException
import org.json.JSONObject
import org.tasks.BuildConfig
import org.tasks.R
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Reads and validates the app configuration from `authConfig`. Configuration
 * changes are detected by comparing the hash of the last known configuration to the read
 * configuration. When a configuration change is detected, the app state is reset.
 */
class Configuration(
        private val context: Context,
        private val authConfig: Int,
        debugConnectionBuilder: DebugConnectionBuilder
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var configJson: JSONObject? = null
    private var configHash: String? = null

    /**
     * Returns a description of the configuration error, if the configuration is invalid.
     */
    var configurationError: String? = null
    var clientId: String? = null
        private set
    private var mScope: String? = null
    private var mRedirectUri: Uri? = null
    var discoveryUri: Uri? = null
        private set
    var authEndpointUri: Uri? = null
        private set
    var tokenEndpointUri: Uri? = null
        private set
    var registrationEndpointUri: Uri? = null
        private set
    var userInfoEndpointUri: Uri? = null
        private set
    private var isHttpsRequired = false

    /**
     * Indicates whether the configuration has changed from the last known valid state.
     */
    fun hasConfigurationChanged(): Boolean = configHash != lastKnownConfigHash

    /**
     * Indicates whether the current configuration is valid.
     */
    val isValid: Boolean
        get() = configurationError == null

    /**
     * Indicates that the current configuration should be accepted as the "last known valid"
     * configuration.
     */
    fun acceptConfiguration() {
        prefs.edit().putString(KEY_LAST_HASH, configHash).apply()
    }

    val scope: String
        get() = mScope!!

    val redirectUri: Uri
        get() = mRedirectUri!!

    val connectionBuilder: ConnectionBuilder = when {
        BuildConfig.DEBUG -> debugConnectionBuilder
        else -> DefaultConnectionBuilder.INSTANCE
    }

    private val lastKnownConfigHash: String?
        get() = prefs.getString(KEY_LAST_HASH, null)

    @Throws(InvalidConfigurationException::class)
    private fun readConfiguration() {
        val configSource = context.resources.openRawResource(authConfig).source().buffer()
        val configData = Buffer()
        configJson = try {
            configSource.readAll(configData)
            JSONObject(configData.readString(StandardCharsets.UTF_8))
        } catch (ex: IOException) {
            throw InvalidConfigurationException(
                    "Failed to read configuration: " + ex.message)
        } catch (ex: JSONException) {
            throw InvalidConfigurationException(
                    "Unable to parse configuration: " + ex.message)
        }
        configHash = configData.sha256().base64()
        clientId = getConfigString("client_id")
        mScope = getRequiredConfigString("authorization_scope")
        mRedirectUri = getRequiredConfigUri("redirect_uri")
        if (!isRedirectUriRegistered) {
            throw InvalidConfigurationException(
                    "redirect_uri is not handled by any activity in this app! "
                            + "Ensure that the appAuthRedirectScheme in your build.gradle file "
                            + "is correctly configured, or that an appropriate intent filter "
                            + "exists in your app manifest.")
        }
        if (getConfigString("discovery_uri") == null) {
            authEndpointUri = getRequiredConfigWebUri("authorization_endpoint_uri")
            tokenEndpointUri = getRequiredConfigWebUri("token_endpoint_uri")
            userInfoEndpointUri = getRequiredConfigWebUri("user_info_endpoint_uri")
            if (clientId == null) {
                registrationEndpointUri = getRequiredConfigWebUri("registration_endpoint_uri")
            }
        } else {
            discoveryUri = getRequiredConfigWebUri("discovery_uri")
            if (BuildConfig.DEBUG) {
                discoveryUri = Uri.parse(discoveryUri.toString().replace(
                        """^https://caldav.tasks.org""".toRegex(),
                        context.getString(R.string.tasks_caldav_url)
                ))
            }
        }
        isHttpsRequired = configJson!!.optBoolean("https_required", true)
    }

    private fun getConfigString(propName: String?): String? {
        var value = configJson!!.optString(propName) ?: return null
        value = value.trim { it <= ' ' }
        return if (TextUtils.isEmpty(value)) null else value
    }

    @Throws(InvalidConfigurationException::class)
    private fun getRequiredConfigString(propName: String): String = getConfigString(propName)
            ?: throw InvalidConfigurationException(
                    "$propName is required but not specified in the configuration")

    @Throws(InvalidConfigurationException::class)
    fun getRequiredConfigUri(propName: String): Uri {
        val uriStr = getRequiredConfigString(propName)
        val uri = try {
            Uri.parse(uriStr)
        } catch (ex: Throwable) {
            throw InvalidConfigurationException("$propName could not be parsed", ex)
        }
        if (!uri.isHierarchical || !uri.isAbsolute) {
            throw InvalidConfigurationException(
                    "$propName must be hierarchical and absolute")
        }
        if (!TextUtils.isEmpty(uri.encodedUserInfo)) {
            throw InvalidConfigurationException("$propName must not have user info")
        }
        if (!TextUtils.isEmpty(uri.encodedQuery)) {
            throw InvalidConfigurationException("$propName must not have query parameters")
        }
        if (!TextUtils.isEmpty(uri.encodedFragment)) {
            throw InvalidConfigurationException("$propName must not have a fragment")
        }
        return uri
    }

    @Throws(InvalidConfigurationException::class)
    fun getRequiredConfigWebUri(propName: String): Uri {
        val uri = getRequiredConfigUri(propName)
        val scheme = uri.scheme
        if (TextUtils.isEmpty(scheme) || !("http" == scheme || "https" == scheme)) {
            throw InvalidConfigurationException(
                    "$propName must have an http or https scheme")
        }
        return uri
    }

    // ensure that the redirect URI declared in the configuration is handled by some activity
    // in the app, by querying the package manager speculatively
    private val isRedirectUriRegistered: Boolean
        get() {
            // ensure that the redirect URI declared in the configuration is handled by some activity
            // in the app, by querying the package manager speculatively
            val redirectIntent = Intent().apply {
                setPackage(context.packageName)
                action = Intent.ACTION_VIEW
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = mRedirectUri
            }
            return context.packageManager.queryIntentActivities(redirectIntent, 0).isNotEmpty()
        }

    class InvalidConfigurationException : Exception {
        internal constructor(reason: String?) : super(reason)
        internal constructor(reason: String?, cause: Throwable?) : super(reason, cause)
    }

    companion object {
        private const val PREFS_NAME = "config"
        private const val KEY_LAST_HASH = "lastHash"
        val GOOGLE_CONFIG = R.raw.google_config
        val GITHUB_CONFIG = R.raw.github_config
    }

    init {
        try {
            readConfiguration()
        } catch (ex: InvalidConfigurationException) {
            configurationError = ex.message
        }
    }
}