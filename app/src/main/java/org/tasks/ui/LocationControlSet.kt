package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.core.content.IntentCompat
import androidx.core.util.Pair
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.compose.edit.LocationRow
import org.tasks.data.Location
import org.tasks.data.createGeofence
import org.tasks.data.displayName
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.open
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.GeofenceDialog
import org.tasks.extensions.Context.openUri
import org.tasks.location.LocationPermissionDialog.Companion.newLocationPermissionDialog
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.PermissionChecker.backgroundPermissions
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class LocationControlSet : TaskEditControlFragment() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var permissionChecker: PermissionChecker

    private fun showGeofenceOptions() {
        val dialog = GeofenceDialog.newGeofenceDialog(viewModel.viewState.value.location)
        dialog.setTargetFragment(this, REQUEST_GEOFENCE_DETAILS)
        dialog.show(parentFragmentManager, FRAG_TAG_LOCATION_DIALOG)
    }

    private fun chooseLocation() {
        val intent = Intent(activity, LocationPickerActivity::class.java)
        viewModel.viewState.value.location?.let {
            intent.putExtra(LocationPickerActivity.EXTRA_PLACE, it.place as Parcelable)
        }
        startActivityForResult(intent, REQUEST_LOCATION_REMINDER)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    override fun Content() {
        val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
        val hasPermissions =
            rememberMultiplePermissionsState(permissions = backgroundPermissions())
                .allPermissionsGranted
        LocationRow(
            location = viewState.location,
            hasPermissions = hasPermissions,
            onClick = {
                viewState.location
                    ?.let { location ->
                        val options: MutableList<Pair<Int, () -> Unit>> = ArrayList()
                        options.add(Pair.create(R.string.open_map) { location.open(activity) })
                        if (!isNullOrEmpty(location.phone)) {
                            options.add(Pair.create(R.string.action_call) { call() })
                        }
                        if (!isNullOrEmpty(location.url)) {
                            options.add(Pair.create(R.string.visit_website) { openWebsite() })
                        }
                        options.add(Pair.create(R.string.choose_new_location) { chooseLocation() })
                        options.add(Pair.create(R.string.delete) {
                            viewModel.setLocation(
                                null
                            )
                        })
                        val items = options.map { requireContext().getString(it.first!!) }
                        dialogBuilder
                            .newDialog(location.displayName)
                            .setItems(items) { _, which: Int ->
                                options[which].second!!()
                            }
                            .show()
                    }
                    ?: chooseLocation()
            },
            openGeofenceOptions = {
                if (hasPermissions) {
                    showGeofenceOptions()
                } else {
                    newLocationPermissionDialog(
                        this@LocationControlSet,
                        REQUEST_LOCATION_PERMISSIONS
                    )
                        .show(parentFragmentManager, FRAG_TAG_REQUEST_LOCATION)
                }
            }
        )
    }

    private fun openWebsite() {
        viewModel.viewState.value.location?.let { context?.openUri(it.url) }
    }

    private fun call() {
        viewModel.viewState.value.location?.let {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + it.phone)))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_LOCATION_PERMISSIONS) {
            if (permissionChecker.canAccessBackgroundLocation()) {
                showGeofenceOptions()
            }
        } else if (requestCode == REQUEST_LOCATION_REMINDER) {
            if (resultCode == Activity.RESULT_OK) {
                val place: Place = data!!.getParcelableExtra(LocationPickerActivity.EXTRA_PLACE)!!
                val location = viewModel.viewState.value.location
                val geofence = if (location == null) {
                    createGeofence(place.uid, preferences)
                } else {
                    val existing = location.geofence
                    Geofence(
                            place = place.uid,
                            isArrival = existing.isArrival,
                            isDeparture = existing.isDeparture,
                    )
                }
                viewModel.setLocation(Location(geofence, place))
            }
        } else if (requestCode == REQUEST_GEOFENCE_DETAILS) {
            if (resultCode == Activity.RESULT_OK) {
                val geofence = data
                    ?.let {
                        IntentCompat.getParcelableExtra(
                            it,
                            GeofenceDialog.EXTRA_GEOFENCE,
                            Geofence::class.java
                        )
                    }
                    ?: return
                viewModel.setLocation(
                    Location(
                        geofence,
                        viewModel.viewState.value.location?.place ?: return
                    )
                )
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        val TAG = R.string.TEA_ctrl_locations_pref
        private const val REQUEST_LOCATION_REMINDER = 12153
        private const val REQUEST_GEOFENCE_DETAILS = 12154
        private const val REQUEST_LOCATION_PERMISSIONS = 12155
        private const val FRAG_TAG_LOCATION_DIALOG = "location_dialog"
        private const val FRAG_TAG_REQUEST_LOCATION = "request_location"
    }
}
