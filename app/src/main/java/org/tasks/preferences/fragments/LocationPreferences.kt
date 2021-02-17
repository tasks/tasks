package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import androidx.preference.SwitchPreference
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.location.LocationPermissionDialog.Companion.newLocationPermissionDialog
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class LocationPreferences : InjectingPreferenceFragment() {

    @Inject lateinit var permissionChecker: PermissionChecker
    @Inject lateinit var preferences: Preferences

    override fun getPreferenceXml() = R.xml.preferences_location

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
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