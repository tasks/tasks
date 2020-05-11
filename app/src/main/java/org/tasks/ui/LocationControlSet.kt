package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.util.Pair
import butterknife.BindView
import butterknife.OnClick
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.SyncFlags
import com.todoroo.astrid.data.Task
import org.tasks.PermissionUtil.verifyPermissions
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.Geofence
import org.tasks.data.Location
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.GeofenceDialog
import org.tasks.injection.FragmentComponent
import org.tasks.location.GeofenceApi
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.*
import java.util.*
import javax.inject.Inject

class LocationControlSet : TaskEditControlFragment() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var geofenceApi: GeofenceApi
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var device: Device
    @Inject lateinit var permissionRequestor: FragmentPermissionRequestor
    @Inject lateinit var permissionChecker: PermissionChecker

    @BindView(R.id.location_name)
    lateinit var locationName: TextView

    @BindView(R.id.location_address)
    lateinit var locationAddress: TextView

    @BindView(R.id.geofence_options)
    lateinit var geofenceOptions: ImageView
    
    private var original: Location? = null
    private var location: Location? = null
    
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        if (savedInstanceState == null) {
            if (task.isNew) {
                if (task.hasTransitory(Place.KEY)) {
                    val place = locationDao.getPlace(task.getTransitory<String>(Place.KEY)!!)
                    if (place != null) {
                        original = Location(Geofence(place.uid, preferences), place)
                    }
                }
            } else {
                original = locationDao.getGeofences(task.id)
            }
            if (original != null) {
                location = Location(original!!.geofence, original!!.place)
            }
        } else {
            original = savedInstanceState.getParcelable(EXTRA_ORIGINAL)
            location = savedInstanceState.getParcelable(EXTRA_LOCATION)
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun setLocation(location: Location?) {
        this.location = location
        updateUi()
    }

    private fun updateUi() {
        if (location == null) {
            locationName.text = ""
            geofenceOptions.visibility = View.GONE
            locationAddress.visibility = View.GONE
        } else {
            geofenceOptions.visibility = if (device.supportsGeofences()) View.VISIBLE else View.GONE
            geofenceOptions.setImageResource(
                    if (permissionChecker.canAccessLocation()
                            && (location!!.isArrival || location!!.isDeparture)) R.drawable.ic_outline_notifications_24px else R.drawable.ic_outline_notifications_off_24px)
            val name = location!!.displayName
            val address = location!!.displayAddress
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
        if (location == null) {
            chooseLocation()
        } else {
            val options: MutableList<Pair<Int, () -> Unit>> = ArrayList()
            options.add(Pair.create(R.string.open_map, { location!!.open(activity) }))
            if (!isNullOrEmpty(location!!.phone)) {
                options.add(Pair.create(R.string.action_call, { call() }))
            }
            if (!isNullOrEmpty(location!!.url)) {
                options.add(Pair.create(R.string.visit_website, { openWebsite() }))
            }
            options.add(Pair.create(R.string.choose_new_location, { chooseLocation() }))
            options.add(Pair.create(R.string.delete, { setLocation(null) }))
            val items = options.map { requireContext().getString(it.first!!) }
            dialogBuilder
                    .newDialog(location!!.displayName)
                    .setItems(items) { _, which: Int ->
                        options[which].second!!.invoke()
                    }
                    .show()
        }
    }

    override val isClickable: Boolean
        get() = true

    private fun chooseLocation() {
        val intent = Intent(activity, LocationPickerActivity::class.java)
        if (location != null) {
            intent.putExtra(LocationPickerActivity.EXTRA_PLACE, location!!.place as Parcelable)
        }
        startActivityForResult(intent, REQUEST_LOCATION_REMINDER)
    }

    @OnClick(R.id.geofence_options)
    fun geofenceOptions() {
        if (permissionRequestor.requestFineLocation()) {
            showGeofenceOptions()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PermissionRequestor.REQUEST_LOCATION) {
            if (verifyPermissions(grantResults)) {
                showGeofenceOptions()
            } else {
                dialogBuilder
                        .newDialog(R.string.missing_permissions)
                        .setMessage(R.string.location_permission_required_geofence)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showGeofenceOptions() {
        val dialog = GeofenceDialog.newGeofenceDialog(location)
        dialog.setTargetFragment(this, REQUEST_GEOFENCE_DETAILS)
        dialog.show(parentFragmentManager, FRAG_TAG_LOCATION_DIALOG)
    }

    override val layout: Int
        get() = R.layout.location_row

    override val icon: Int
        get() = R.drawable.ic_outline_place_24px

    override fun controlId() = TAG

    private fun openWebsite() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(location!!.url)))
    }

    private fun call() {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + location!!.phone)))
    }

    override fun hasChanges(task: Task): Boolean {
        if (original == null) {
            return location != null
        }
        if (location == null) {
            return true
        }
        return if (original!!.place != location!!.place) {
            true
        } else {
            original!!.isDeparture != location!!.isDeparture
                    || original!!.isArrival != location!!.isArrival
                    || original!!.radius != location!!.radius
        }
    }

    override fun requiresId() = true

    override fun apply(task: Task) {
        if (original == null || location == null || original!!.place != location!!.place) {
            task.putTransitory(SyncFlags.FORCE_CALDAV_SYNC, true)
        }
        if (original != null) {
            locationDao.delete(original!!.geofence)
            geofenceApi.update(original!!.place)
        }
        if (location != null) {
            val place = location!!.place
            val geofence = location!!.geofence
            geofence.task = task.id
            geofence.place = place.uid
            geofence.id = locationDao.insert(geofence)
            geofenceApi.update(place)
        }
        task.modificationDate = DateUtilities.now()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_ORIGINAL, original)
        outState.putParcelable(EXTRA_LOCATION, location)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_LOCATION_REMINDER) {
            if (resultCode == Activity.RESULT_OK) {
                val place: Place = data!!.getParcelableExtra(LocationPickerActivity.EXTRA_PLACE)!!
                val geofence = if (location == null) {
                    Geofence(place.uid, preferences)
                } else {
                    val existing = location!!.geofence
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
                location!!.geofence = data!!.getParcelableExtra(GeofenceDialog.EXTRA_GEOFENCE)!!
                updateUi()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun inject(component: FragmentComponent) = component.inject(this)

    companion object {
        const val TAG = R.string.TEA_ctrl_locations_pref
        private const val REQUEST_LOCATION_REMINDER = 12153
        private const val REQUEST_GEOFENCE_DETAILS = 12154
        private const val FRAG_TAG_LOCATION_DIALOG = "location_dialog"
        private const val EXTRA_ORIGINAL = "extra_original_location"
        private const val EXTRA_LOCATION = "extra_new_location"
    }
}