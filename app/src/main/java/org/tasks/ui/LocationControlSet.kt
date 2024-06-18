package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
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
import org.tasks.themes.TasksTheme
import javax.inject.Inject

@AndroidEntryPoint
class LocationControlSet : TaskEditControlFragment() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var permissionChecker: PermissionChecker

    private fun setLocation(location: Location?) {
        viewModel.selectedLocation.value = location
    }

    private fun onRowClick() {
        val location = viewModel.selectedLocation.value
        if (location == null) {
            chooseLocation()
        } else {
            val options: MutableList<Pair<Int, () -> Unit>> = ArrayList()
            options.add(Pair.create(R.string.open_map) { location.open(activity) })
            if (!isNullOrEmpty(location.phone)) {
                options.add(Pair.create(R.string.action_call) { call() })
            }
            if (!isNullOrEmpty(location.url)) {
                options.add(Pair.create(R.string.visit_website) { openWebsite() })
            }
            options.add(Pair.create(R.string.choose_new_location) { chooseLocation() })
            options.add(Pair.create(R.string.delete) { setLocation(null) })
            val items = options.map { requireContext().getString(it.first!!) }
            dialogBuilder
                    .newDialog(location.displayName)
                    .setItems(items) { _, which: Int ->
                        options[which].second!!()
                    }
                    .show()
        }
    }

    private fun chooseLocation() {
        val intent = Intent(activity, LocationPickerActivity::class.java)
        viewModel.selectedLocation.value?.let {
            intent.putExtra(LocationPickerActivity.EXTRA_PLACE, it.place as Parcelable)
        }
        startActivityForResult(intent, REQUEST_LOCATION_REMINDER)
    }

    private fun showGeofenceOptions() {
        val dialog = GeofenceDialog.newGeofenceDialog(viewModel.selectedLocation.value)
        dialog.setTargetFragment(this, REQUEST_GEOFENCE_DETAILS)
        dialog.show(parentFragmentManager, FRAG_TAG_LOCATION_DIALOG)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                TasksTheme {
                    val hasPermissions =
                        rememberMultiplePermissionsState(permissions = backgroundPermissions())
                            .allPermissionsGranted
                    LocationRow(
                        location = viewModel.selectedLocation.collectAsStateWithLifecycle().value,
                        hasPermissions = hasPermissions,
                        onClick = this@LocationControlSet::onRowClick,
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
            }
        }

    override fun controlId() = TAG

    private fun openWebsite() {
        viewModel.selectedLocation.value?.let { context?.openUri(it.url) }
    }

    private fun call() {
        viewModel.selectedLocation.value?.let {
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
                val location = viewModel.selectedLocation.value
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
                setLocation(Location(geofence, place))
            }
        } else if (requestCode == REQUEST_GEOFENCE_DETAILS) {
            if (resultCode == Activity.RESULT_OK) {
                setLocation(Location(
                        data?.getParcelableExtra(GeofenceDialog.EXTRA_GEOFENCE) ?: return,
                        viewModel.selectedLocation.value?.place ?: return
                ))
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
