package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.util.Pair
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.compose.DisabledText
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.data.Geofence
import org.tasks.data.Location
import org.tasks.data.Place
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.GeofenceDialog
import org.tasks.extensions.Context.openUri
import org.tasks.location.LocationPermissionDialog.Companion.newLocationPermissionDialog
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.Device
import org.tasks.preferences.FragmentPermissionRequestor
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.PermissionChecker.backgroundPermissions
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class LocationControlSet : TaskEditControlComposeFragment() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var device: Device
    @Inject lateinit var permissionRequestor: FragmentPermissionRequestor
    @Inject lateinit var permissionChecker: PermissionChecker

    private fun setLocation(location: Location?) {
        viewModel.selectedLocation.value = location
    }

    override fun onRowClick() {
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
    @Composable
    override fun Body() {
        val location = viewModel.selectedLocation.collectAsStateLifecycleAware().value
        val hasPermissions =
            rememberMultiplePermissionsState(permissions = backgroundPermissions())
                .allPermissionsGranted
        if (location == null) {
            DisabledText(
                text = stringResource(id = R.string.add_location),
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            LocationRow(
                name = location.displayName,
                address = location.displayAddress,
                onClick = {
                    if (hasPermissions) {
                        showGeofenceOptions()
                    } else {
                        newLocationPermissionDialog(this, REQUEST_LOCATION_PERMISSIONS)
                            .show(parentFragmentManager, FRAG_TAG_REQUEST_LOCATION)
                    }
                },
                geofenceOn = hasPermissions && (location.isArrival || location.isDeparture)
            )
        }
    }

    override val icon = R.drawable.ic_outline_place_24px

    override fun controlId() = TAG

    override val isClickable = true

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
                    Geofence(place.uid, preferences)
                } else {
                    val existing = location.geofence
                    Geofence(
                            place.uid,
                            existing.isArrival,
                            existing.isDeparture
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
        const val TAG = R.string.TEA_ctrl_locations_pref
        private const val REQUEST_LOCATION_REMINDER = 12153
        private const val REQUEST_GEOFENCE_DETAILS = 12154
        private const val REQUEST_LOCATION_PERMISSIONS = 12155
        private const val FRAG_TAG_LOCATION_DIALOG = "location_dialog"
        private const val FRAG_TAG_REQUEST_LOCATION = "request_location"
    }
}

@Composable
fun LocationRow(
    name: String,
    address: String?,
    geofenceOn: Boolean,
    onClick: () -> Unit,
) {
    Row {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 20.dp)
        ) {
            Text(text = name)
            address?.takeIf { it.isNotBlank() && it != name }?.let {
                Text(text = address)
            }
        }
        IconButton(
            onClick = onClick,
            modifier = Modifier.padding(top = 8.dp /* + 12dp from icon */)
        ) {
            Icon(
                imageVector = if (geofenceOn) {
                    Icons.Outlined.Notifications
                } else {
                    Icons.Outlined.NotificationsOff
                },
                contentDescription = null
            )
        }
    }
}

