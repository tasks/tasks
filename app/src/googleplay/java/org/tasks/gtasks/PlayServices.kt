package org.tasks.gtasks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.auth.GoogleSignInAccount
import org.tasks.auth.OauthSignIn
import org.tasks.data.LocationDao
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

class PlayServices @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val locationDao: LocationDao) {

    suspend fun check(activity: Activity?) {
        val playServicesAvailable = locationDao.geofenceCount() == 0 || refreshAndCheck()
        if (!playServicesAvailable && !preferences.getBoolean(R.string.warned_play_services, false)) {
            preferences.setBoolean(R.string.warned_play_services, true)
            resolve(activity)
        }
    }

    fun refreshAndCheck(): Boolean {
        refresh()
        return isPlayServicesAvailable
    }

    val isPlayServicesAvailable: Boolean
        get() = result == ConnectionResult.SUCCESS

    private fun refresh() {
        val instance = GoogleApiAvailability.getInstance()
        val googlePlayServicesAvailable = instance.isGooglePlayServicesAvailable(context)
        preferences.setInt(R.string.play_services_available, googlePlayServicesAvailable)
        if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
            preferences.setBoolean(R.string.warned_play_services, false)
        }
        Timber.d(status)
    }

    fun resolve(activity: Activity?) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val error = preferences.getInt(R.string.play_services_available, -1)
        if (googleApiAvailability.isUserResolvableError(error)) {
            googleApiAvailability.getErrorDialog(activity, error, REQUEST_RESOLUTION).show()
        } else {
            Toast.makeText(activity, status, Toast.LENGTH_LONG).show()
        }
    }

    suspend fun getSignedInAccount(): OauthSignIn? {
        return withContext(Dispatchers.IO) {
            try {
                Tasks
                        .await(client.silentSignIn())
                        ?.let { GoogleSignInAccount(it) }
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }
    }

    fun signInFromIntent(data: Intent?): OauthSignIn? = try {
        GoogleSignIn
                .getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
                ?.let { GoogleSignInAccount(it) }
    } catch (e: ApiException) {
        Timber.e(e)
        null
    }

    val signInIntent: Intent
        get() = client.signInIntent

    private val client: GoogleSignInClient
        get() = GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken(context.getString(R.string.google_sign_in))
                        .build())

    private val status: String
        get() = GoogleApiAvailability.getInstance().getErrorString(result)

    private val result: Int
        get() = preferences.getInt(R.string.play_services_available, -1)

    companion object {
        private const val REQUEST_RESOLUTION = 10000
    }
}