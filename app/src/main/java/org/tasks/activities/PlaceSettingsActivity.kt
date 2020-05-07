package org.tasks.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import butterknife.BindView
import butterknife.OnTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.filters.PlaceFilter
import org.tasks.injection.ActivityComponent
import org.tasks.location.MapFragment
import javax.inject.Inject

class PlaceSettingsActivity : BaseListSettingsActivity(), MapFragment.MapFragmentCallback {

    companion object {
        const val EXTRA_PLACE = "extra_place"
    }

    @BindView(R.id.name) lateinit var name: TextInputEditText
    @BindView(R.id.name_layout) lateinit var nameLayout: TextInputLayout

    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var map: MapFragment

    private lateinit var place: Place

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent?.hasExtra(EXTRA_PLACE) != true) {
            finish()
        }

        val extra: Place? = intent?.getParcelableExtra(EXTRA_PLACE)
        if (extra == null) {
            finish()
            return
        }

        place = extra

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            name.setText(place.displayName)
            selectedColor = place.color
            selectedIcon = place.getIcon()!!
        }

        map.init(supportFragmentManager, this, tasksTheme.themeBase.isDarkTheme(this))

        updateTheme()
    }

    override fun getLayout() = R.layout.activity_location_settings

    override fun hasChanges() = name.text.toString() != place.displayName
                    || selectedColor != place.color
                    || selectedIcon != place.getIcon()!!

    @OnTextChanged(R.id.name)
    fun onNameChanged() {
        nameLayout.error = null
    }

    override fun save() {
        val newName: String = name.text.toString()

        if (isNullOrEmpty(newName)) {
            nameLayout.error = getString(R.string.name_cannot_be_empty)
            return
        }

        place.name = newName
        place.color = selectedColor
        place.setIcon(selectedIcon)
        locationDao.update(place)
        setResult(
                Activity.RESULT_OK,
                Intent(TaskListFragment.ACTION_RELOAD)
                        .putExtra(MainActivity.OPEN_FILTER, PlaceFilter(place)))
        finish()
    }

    override fun isNew() = false

    override fun getToolbarTitle(): String? {
        return place.address ?: place.displayName
    }

    override fun delete() {
        locationDao.deleteGeofencesByPlace(place.uid!!)
        locationDao.delete(place)
        setResult(Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED))
        finish()
    }

    override fun inject(component: ActivityComponent) = component.inject(this)

    override fun onMapReady(mapFragment: MapFragment?) {
        map = mapFragment!!
        map.setMarkers(listOf(place))
        map.disableGestures()
        map.movePosition(place.mapPosition, false)
    }

    override fun onPlaceSelected(place: Place?) {}
}