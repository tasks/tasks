package org.tasks.location

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.Behavior.DragCallback
import com.google.android.material.appbar.CollapsingToolbarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tasks.Event
import org.tasks.PermissionUtil.verifyPermissions
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.activities.PlaceSettingsActivity
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.caldav.GeoUtils.toLikeString
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.data.PlaceUsage
import org.tasks.databinding.ActivityLocationPickerBinding
import org.tasks.dialogs.DialogBuilder
import org.tasks.extensions.Context.toast
import org.tasks.extensions.hideKeyboard
import org.tasks.extensions.setOnQueryTextListener
import org.tasks.location.LocationPickerAdapter.OnLocationPicked
import org.tasks.location.LocationSearchAdapter.OnPredictionPicked
import org.tasks.location.MapFragment.MapFragmentCallback
import org.tasks.preferences.ActivityPermissionRequestor
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.PermissionRequestor
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.themes.Theme
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class LocationPickerActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener, MapFragmentCallback, OnLocationPicked, SearchView.OnQueryTextListener, OnPredictionPicked, MenuItem.OnActionExpandListener {
    private lateinit var toolbar: Toolbar
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbarLayout: CollapsingToolbarLayout
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var searchView: View
    private lateinit var loadingIndicator: ContentLoadingProgressBar
    private lateinit var chooseRecentLocation: View
    private lateinit var recyclerView: RecyclerView

    @Inject lateinit var theme: Theme
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var permissionChecker: PermissionChecker
    @Inject lateinit var permissionRequestor: ActivityPermissionRequestor
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var map: MapFragment
    @Inject lateinit var geocoder: Geocoder
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var locationService: LocationService
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var preferences: Preferences

    private var mapPosition: MapPosition? = null
    private var recentsAdapter: LocationPickerAdapter? = null
    private var searchAdapter: LocationSearchAdapter? = null
    private var places: List<PlaceUsage> = emptyList()
    private var offset = 0
    private lateinit var search: MenuItem
    private var searchJob: Job? = null
    private val viewModel: PlaceSearchViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.applyTheme(this)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        val binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        toolbar = binding.toolbar
        appBarLayout = binding.appBarLayout
        toolbarLayout = binding.collapsingToolbarLayout
        coordinatorLayout = binding.coordinator
        searchView = binding.search.apply {
            setOnClickListener { searchPlace() }
        }
        loadingIndicator = binding.loadingIndicator
        chooseRecentLocation = binding.chooseRecentLocation
        recyclerView = binding.recentLocations
        val configuration = resources.configuration
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                && configuration.smallestScreenWidthDp < 480) {
            searchView.visibility = View.GONE
        }
        val currentPlace: Place? = intent.getParcelableExtra(EXTRA_PLACE)
        if (savedInstanceState == null) {
            mapPosition = currentPlace?.mapPosition ?: intent.getParcelableExtra(EXTRA_MAP_POSITION)
        } else {
            mapPosition = savedInstanceState.getParcelable(EXTRA_MAP_POSITION)
            offset = savedInstanceState.getInt(EXTRA_APPBAR_OFFSET)
            viewModel.restoreState(savedInstanceState)
        }
        toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24px)
        toolbar.setNavigationOnClickListener { collapseToolbar() }
        toolbar.inflateMenu(R.menu.menu_location_picker)
        val menu = toolbar.menu
        search = menu.findItem(R.id.menu_search)
        search.setOnActionExpandListener(this)
        toolbar.setOnMenuItemClickListener(this)
        val themeColor = theme.themeColor
        themeColor.applyToNavigationBar(this)
        val dark = preferences.mapTheme == 2
                || preferences.mapTheme == 0 && theme.themeBase.isDarkTheme(this)
        map.init(this, this, dark)
        val params = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = AppBarLayout.Behavior()
        behavior.setDragCallback(
                object : DragCallback() {
                    override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                        return false
                    }
                })
        params.behavior = behavior
        appBarLayout.addOnOffsetChangedListener { appBarLayout: AppBarLayout, offset: Int ->
            if (offset == 0 && this.offset != 0) {
                closeSearch()
                hideKeyboard()
            }
            this.offset = offset
            toolbar.alpha = abs(offset / appBarLayout.totalScrollRange.toFloat())
        }
        coordinatorLayout.addOnLayoutChangeListener(
                object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                            v: View, l: Int, t: Int, r: Int, b: Int, ol: Int, ot: Int, or: Int, ob: Int) {
                        coordinatorLayout.removeOnLayoutChangeListener(this)
                        locationDao
                                .getPlaceUsage()
                                .observe(this@LocationPickerActivity) {
                                    places: List<PlaceUsage> -> updatePlaces(places)
                                }
                    }
                })
        if (offset != 0) {
            appBarLayout.post { expandToolbar(false) }
        }
        findViewById<View>(R.id.google_marker).visibility = View.VISIBLE
        searchAdapter = LocationSearchAdapter(viewModel.getAttributionRes(dark), this)
        recentsAdapter = LocationPickerAdapter(this, inventory, colorProvider, this)
        recentsAdapter!!.setHasStableIds(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = if (search.isActionViewExpanded) searchAdapter else recentsAdapter

        binding.currentLocation.setOnClickListener { currentLocation() }
        binding.selectThisLocation.setOnClickListener { selectLocation() }
    }

    override fun onMapReady(mapFragment: MapFragment) {
        map = mapFragment
        updateMarkers()
        if (permissionChecker.canAccessForegroundLocation()) {
            mapFragment.showMyLocation()
        }

        mapPosition
                ?.let { map.movePosition(it, false) }
                ?: moveToCurrentLocation(false)
    }

    override fun onBackPressed() {
        if (closeSearch()) {
            return
        }
        if (offset != 0) {
            collapseToolbar()
            return
        }
        super.onBackPressed()
    }

    private fun closeSearch(): Boolean = search.isActionViewExpanded && search.collapseActionView()

    override fun onPlaceSelected(place: Place) {
        returnPlace(place)
    }

    private fun currentLocation() {
        if (permissionRequestor.requestForegroundLocation()) {
            moveToCurrentLocation(true)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PermissionRequestor.REQUEST_FOREGROUND_LOCATION) {
            if (verifyPermissions(grantResults)) {
                map.showMyLocation()
                moveToCurrentLocation(true)
            } else {
                dialogBuilder
                        .newDialog(R.string.missing_permissions)
                        .setMessage(R.string.location_permission_required_location)
                        .setPositiveButton(R.string.ok, null)
                        .show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun selectLocation() {
        val mapPosition = map.mapPosition ?: return
        loadingIndicator.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                returnPlace(
                    geocoder.reverseGeocode(mapPosition)
                        ?: Place(
                            latitude = mapPosition.latitude,
                            longitude = mapPosition.longitude,
                        )
                )
            } catch (e: Exception) {
                loadingIndicator.visibility = View.GONE
                firebase.reportException(e)
                toast(e.message)
            }
        }
    }

    private fun searchPlace() {
        mapPosition = map.mapPosition
        expandToolbar(true)
        search.expandActionView()
    }

    private fun moveToCurrentLocation(animate: Boolean) {
        if (!permissionChecker.canAccessForegroundLocation()) {
            return
        }
        lifecycleScope.launch {
            try {
                locationService.currentLocation()?.let { map.movePosition(it, animate) }
            } catch (e: Exception) {
                toast(e.message)
            }
        }
    }

    private fun returnPlace(place: Place?) {
        if (place == null) {
            Timber.e("Place is null")
            return
        }
        hideKeyboard()
        lifecycleScope.launch {
            var place = place
            if (place.id <= 0) {
                place = locationDao
                    .findPlace(
                        place.latitude.toLikeString(),
                        place.longitude.toLikeString()
                    )
                    ?: place.copy(id = locationDao.insert(place))
            }
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_PLACE, place as Parcelable?))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        viewModel.observe(
            this,
            { searchAdapter!!.submitList(it) },
            { returnPlace(it) },
            { handleError(it) }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDestroy()
    }

    private fun handleError(error: Event<String>) {
        val message = error.ifUnhandled
        if (!isNullOrEmpty(message)) {
            toast(message)
        }
    }

    private fun updatePlaces(places: List<PlaceUsage>) {
        this.places = places
        updateMarkers()
        recentsAdapter!!.submitList(places)
        val params = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        val height = coordinatorLayout.height
        if (this.places.isEmpty()) {
            params.height = height
            chooseRecentLocation.visibility = View.GONE
            collapseToolbar()
        } else {
            params.height = height * 75 / 100
            chooseRecentLocation.visibility = View.VISIBLE
        }
    }

    private fun updateMarkers() {
        map.setMarkers(places.map(PlaceUsage::place))
    }

    private fun collapseToolbar() {
        appBarLayout.setExpanded(true, true)
    }

    private fun expandToolbar(animate: Boolean) {
        appBarLayout.setExpanded(false, animate)
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_MAP_POSITION, map.mapPosition)
        outState.putInt(EXTRA_APPBAR_OFFSET, offset)
        viewModel.saveState(outState)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean =
            if (item.itemId == R.id.menu_search) {
                searchPlace()
                true
            } else false

    override fun picked(place: Place) {
        returnPlace(place)
    }

    override fun settings(place: Place) {
        val intent = Intent(this, PlaceSettingsActivity::class.java)
        intent.putExtra(PlaceSettingsActivity.EXTRA_PLACE, place as Parcelable)
        startActivity(intent)
    }

    override fun onQueryTextSubmit(query: String): Boolean = false

    override fun onQueryTextChange(query: String): Boolean {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(SEARCH_DEBOUNCE_TIMEOUT)
            viewModel.query(query, map.mapPosition)
        }
        return true
    }

    override fun picked(prediction: PlaceSearchResult) {
        viewModel.fetch(prediction)
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        search.setOnQueryTextListener(this)
        searchAdapter!!.submitList(emptyList())
        recyclerView.adapter = searchAdapter
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        search.setOnQueryTextListener(null)
        recyclerView.adapter = recentsAdapter
        if (places.isEmpty()) {
            collapseToolbar()
        }
        return true
    }

    companion object {
        const val EXTRA_PLACE = "extra_place"
        private const val EXTRA_MAP_POSITION = "extra_map_position"
        private const val EXTRA_APPBAR_OFFSET = "extra_appbar_offset"
        private const val SEARCH_DEBOUNCE_TIMEOUT = 300L
    }
}