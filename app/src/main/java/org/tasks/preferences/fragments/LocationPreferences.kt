package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.Tasks.Companion.IS_GOOGLE_PLAY
import org.tasks.billing.Inventory
import org.tasks.gtasks.PlayServices
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.location.GeofenceApi
import org.tasks.location.LocationPermissionDialog.Companion.newLocationPermissionDialog
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.ui.Toaster
import javax.inject.Inject

@AndroidEntryPoint
class LocationPreferences : InjectingPreferenceFragment() {

    @Inject lateinit var playServices: PlayServices
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var toaster: Toaster
    @Inject lateinit var geofenceApi: GeofenceApi
    @Inject lateinit var permissionChecker: PermissionChecker
    @Inject lateinit var preferences: Preferences

    override fun getPreferenceXml() = R.xml.preferences_location

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        if (IS_GOOGLE_PLAY) {
            findPreference(R.string.p_geofence_service)
                    .setOnPreferenceChangeListener(this::onGeofenceServiceChanged)
        } else {
            disable(R.string.p_geofence_service)
        }
    }

    override fun onResume() {
        super.onResume()

        updatePermissions()
    }

    private fun updatePermissions() {
        val hasPermissions = permissionChecker.canAccessBackgroundLocation()
        preferences.setBoolean(R.string.p_location_based_reminders, hasPermissions)
        with((findPreference(R.string.p_location_based_reminders) as SwitchPreference)) {
            isChecked = hasPermissions
            isEnabled = !hasPermissions
            setOnPreferenceClickListener {
                if (!permissionChecker.canAccessBackgroundLocation()) {
                    newLocationPermissionDialog(this@LocationPreferences, REQUEST_BACKGROUND_LOCATION)
                            .show(parentFragmentManager, FRAG_TAG_LOCATION_PERMISSION)
                }
                false
            }
        }
        findPreference(R.string.p_geofence_service).isEnabled = hasPermissions && IS_GOOGLE_PLAY
    }

    private fun onGeofenceServiceChanged(preference: Preference, newValue: Any): Boolean =
            if (newValue.toString().toIntOrNull() ?: 0 == 1) {
                if (!playServices.refreshAndCheck()) {
                    playServices.resolve(activity)
                    false
                } else {
                    geofenceChanged()
                }
            } else {
                geofenceChanged()
            }

    private fun geofenceChanged(): Boolean {
        lifecycleScope.launch {
            withContext(NonCancellable) {
                geofenceApi.cancelAll()
            }
            showRestartDialog()
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
            when (requestCode) {
                REQUEST_BACKGROUND_LOCATION -> updatePermissions()
                else -> super.onActivityResult(requestCode, resultCode, data)
            }

    companion object {
        private const val FRAG_TAG_LOCATION_PERMISSION = "frag_tag_location_permissions"
        private const val REQUEST_BACKGROUND_LOCATION = 10101
    }
}