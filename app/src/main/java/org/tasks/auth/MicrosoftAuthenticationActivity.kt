package org.tasks.auth

import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceDiscovery
import okhttp3.Request
import org.json.JSONObject
import org.tasks.R
import org.tasks.analytics.Constants
import org.tasks.analytics.Firebase
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.CaldavDao
import org.tasks.http.HttpClientFactory
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.microsoft.requestTokenExchange
import javax.inject.Inject

@AndroidEntryPoint
class MicrosoftAuthenticationActivity : ComponentActivity() {

    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var encryption: KeyStoreEncryption
    @Inject lateinit var httpClientFactory: HttpClientFactory
    @Inject lateinit var firebase: Firebase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authState = AuthState(
            AuthorizationResponse.fromIntent(intent),
            AuthorizationException.fromIntent(intent)
        )
        authState.authorizationException?.let {
            error(it.message ?: "Authentication failed")
            return
        }
        lifecycleScope.launch {
            val (resp, ex) = requestTokenExchange(authState.lastAuthorizationResponse!!)
            authState.update(resp, ex)
            if (authState.isAuthorized) {
                val email = getEmail(authState.accessToken) ?: run {
                    error("Failed to fetch profile")
                    return@launch
                }
                caldavDao
                    .getAccount(TYPE_MICROSOFT, email)
                    ?.let {
                        it.password = encryption.encrypt(authState.jsonSerializeString())
                        caldavDao.update(it)
                    }
                    ?: caldavDao
                        .insert(
                            CaldavAccount().apply {
                                uuid = UUIDHelper.newUUID()
                                name = email
                                username = email
                                password = encryption.encrypt(authState.jsonSerializeString())
                                accountType = TYPE_MICROSOFT
                            }
                        )
                        .also {
                            firebase.logEvent(
                                R.string.event_sync_add_account,
                                R.string.param_type to Constants.SYNC_TYPE_MICROSOFT
                            )
                        }
                finish()
            } else {
                error(ex?.message ?: "Token exchange failed")
            }
        }
        setContent {
            var showDialog by remember { mutableStateOf(true) }
            if (showDialog) {
                Dialog(
                    onDismissRequest = { showDialog = false },
                    DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .background(White, shape = RoundedCornerShape(8.dp))
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    private suspend fun getEmail(accessToken: String?): String? = withContext(Dispatchers.IO) {
        if (accessToken == null) {
            return@withContext null
        }
        val discovery = AuthorizationServiceDiscovery(
            JSONObject(
                intent.getStringExtra(EXTRA_SERVICE_DISCOVERY)!!
            )
        )
        val userInfo = httpClientFactory
            .newClient(foreground = false)
            .newCall(
                Request.Builder()
                    .url(discovery.userinfoEndpoint!!.toString())
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()
            )
            .execute()
        val response = userInfo.body?.string() ?: return@withContext null
        JSONObject(response).getString("email")
    }

    private fun error(message: String) {
        Toast.makeText(this@MicrosoftAuthenticationActivity, message, LENGTH_LONG).show()
        finish()
    }

    companion object {
        const val EXTRA_SERVICE_DISCOVERY = "extra_service_discovery"
    }
}

