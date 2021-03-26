package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.util.Pair
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.Geofence
import org.tasks.data.Location
import org.tasks.data.Place
import org.tasks.databinding.LocationRowBinding
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.GeofenceDialog
import org.tasks.extensions.Context.openUri
import org.tasks.location.LocationPermissionDialog.Companion.newLocationPermissionDialog
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.Device
import org.tasks.preferences.FragmentPermissionRequestor
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LocationControlSet : TaskEditControlFragment() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var device: Device
    @Inject lateinit var permissionRequestor: FragmentPermissionRequestor
    @Inject lateinit var permissionChecker: PermissionChecker

    private lateinit var locationName: TextView
    private lateinit var locationAddress: TextView
    private lateinit var geofenceOptions: ImageView

    override fun onResume() {
        super.onResume()

        updateUi()
    }

    private fun setLocation(location: Location?) {
        viewModel.selectedLocation = location
        updateUi()
    }

    private fun updateUi() {
        val location = viewModel.selectedLocation
        if (location == null) {
            locationName.text = ""
            geofenceOptions.visibility = View.GONE
            locationAddress.visibility = View.GONE
        } else {
            geofenceOptions.visibility = View.VISIBLE
            geofenceOptions.setImageResource(
                    if (permissionChecker.canAccessBackgroundLocation()
                            && (location.isArrival || location.isDeparture)) R.drawable.ic_outline_notifications_24px else R.drawable.ic_outline_notifications_off_24px)
            val name = location.displayName
            val address = location.displayAddress
            if (!isNullOrEmpty(address) && address != name) {
                locationAddress.text = address
                locationAddress.visibility = View.VISIBLE
            } else {
                locationAddress.visibility = View.GONE
            }
            val spannableString = SpannableString(name)
            spannableString.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(view: View) {}
                    },
                    0,
                    name.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            locationName.text = spannableString
        }
    }

    override fun onRowClick() {
        val location = viewModel.selectedLocation
        if (location == null) {
            chooseLocation()
        } else {
            val options: MutableList<Pair<Int, () -> Unit>> = ArrayList()
            options.add(Pair.create(R.string.open_map, { location.open(activity) }))
            if (!isNullOrEmpty(location.phone)) {
                options.add(Pair.create(R.string.action_call, { call() }))
            }
            if (!isNullOrEmpty(location.url)) {
                options.add(Pair.create(R.string.visit_website, { openWebsite() }))
            }
            options.add(Pair.create(R.string.choose_new_location, { chooseLocation() }))
            options.add(Pair.create(R.string.delete, { setLocation(null) }))
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
        viewModel.selectedLocation?.let {
            intent.putExtra(LocationPickerActivity.EXTRA_PLACE, it.place as Parcelable)
        }
        startActivityForResult(intent, REQUEST_LOCATION_REMINDER)
    }

    private fun geofenceOptions() {
        if (permissionChecker.canAccessBackgroundLocation()) {
            showGeofenceOptions()
        } else {
            newLocationPermissionDialog(this, REQUEST_LOCATION_PERMISSIONS)
                    .show(parentFragmentManager, FRAG_TAG_REQUEST_LOCATION)
        }
    }

    private fun showGeofenceOptions() {
        val dialog = GeofenceDialog.newGeofenceDialog(viewModel.selectedLocation)
        dialog.setTargetFragment(this, REQUEST_GEOFENCE_DETAILS)
        dialog.show(parentFragmentManager, FRAG_TAG_LOCATION_DIALOG)
    }

    override fun bind(parent: ViewGroup?) =
        LocationRowBinding.inflate(layoutInflater, parent, true).let {
            locationName = it.locationName
            locationAddress = it.locationAddress
            geofenceOptions = it.geofenceOptions.apply {
                setOnClickListener { geofenceOptions() }
            }
            it.root
        }

    override val icon = R.drawable.ic_outline_place_24px

    override fun controlId() = TAG

    override val isClickable = true

    private fun openWebsite() {
        viewModel.selectedLocation?.let { context?.openUri(it.url) }
    }

    private fun call() {
        viewModel.selectedLocation?.let {
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
                val location = viewModel.selectedLocation
                val geofence = if (location == null) {
                    Geofence(place.uid, preferences)
                } else {
                    val existing = location.geofence
                    Geofence(
                            place.uid,
                            existing.isArrival,
                            existing.isDeparture,
                            existing.radius)
                }
                setLocation(Location(geofence, place))
            }
        } else if (requestCode == REQUEST_GEOFENCE_DETAILS) {
            if (resultCode == Activity.RESULT_OK) {
                setLocation(Location(
                        data?.getParcelableExtra(GeofenceDialog.EXTRA_GEOFENCE) ?: return,
                        viewModel.selectedLocation?.place ?: return
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