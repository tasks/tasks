package org.tasks.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.databinding.ActivityLocationSettingsBinding
import org.tasks.extensions.formatNumber
import org.tasks.filters.PlaceFilter
import org.tasks.location.MapFragment
import org.tasks.preferences.Preferences
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class PlaceSettingsActivity : BaseListSettingsActivity(), MapFragment.MapFragmentCallback {

    companion object {
        const val EXTRA_PLACE = "extra_place"
        private const val MIN_RADIUS = 75
        private const val MAX_RADIUS = 1000
        private const val STEP = 25.0
    }

    private lateinit var name: TextInputEditText
    private lateinit var nameLayout: TextInputLayout
    private lateinit var slider: Slider

    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var map: MapFragment
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var locale: Locale
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

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
            selectedIcon = place.getIcon()
        }

        val dark = preferences.mapTheme == 2
                || preferences.mapTheme == 0 && tasksTheme.themeBase.isDarkTheme(this)

        map.init(this, this, dark)

        updateTheme()
    }

    override fun bind() = ActivityLocationSettingsBinding.inflate(layoutInflater).let {
        name = it.name.apply {
            addTextChangedListener(
                onTextChanged = { _, _, _, _ -> nameLayout.error = null }
            )
        }
        nameLayout = it.nameLayout
        slider = it.slider.apply {
            setLabelFormatter { value ->
                getString(
                    R.string.location_radius_meters,
                    locale.formatNumber(value.toInt())
                )
            }
            valueTo = MAX_RADIUS.toFloat()
            valueFrom = MIN_RADIUS.toFloat()
            stepSize = STEP.toFloat()
            haloRadius = 0
            value = (place.radius / STEP * STEP).roundToInt().toFloat()
        }
        it.root
    }

    override fun hasChanges() = name.text.toString() != place.displayName
                    || selectedColor != place.color
                    || selectedIcon != place.getIcon()

    override suspend fun save() {
        val newName: String = name.text.toString()

        if (isNullOrEmpty(newName)) {
            nameLayout.error = getString(R.string.name_cannot_be_empty)
            return
        }

        place.name = newName
        place.color = selectedColor
        place.setIcon(selectedIcon)
        place.radius = slider.value.toInt()
        locationDao.update(place)
        setResult(
                Activity.RESULT_OK,
                Intent(TaskListFragment.ACTION_RELOAD)
                        .putExtra(MainActivity.OPEN_FILTER, PlaceFilter(place)))
        finish()
    }

    override val isNew: Boolean
        get() = false

    override val toolbarTitle: String?
        get() = place.address ?: place.displayName

    override suspend fun delete() {
        locationDao.deleteGeofencesByPlace(place.uid!!)
        locationDao.delete(place)
        setResult(Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED))
        localBroadcastManager.broadcastRefreshList()
        finish()
    }

    override fun onMapReady(mapFragment: MapFragment) {
        map = mapFragment
        map.setMarkers(listOf(place))
        map.disableGestures()
        map.movePosition(place.mapPosition, false)
    }

    override fun onPlaceSelected(place: Place) {}
}