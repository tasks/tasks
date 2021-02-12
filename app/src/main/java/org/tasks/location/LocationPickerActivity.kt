package org.tasks.location

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.ContentLoadingProgressBar
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.Behavior.DragCallback
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import org.tasks.Event
import org.tasks.PermissionUtil.verifyPermissions
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.activities.PlaceSettingsActivity
import org.tasks.billing.Inventory
import org.tasks.caldav.GeoUtils.toLikeString
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.data.PlaceUsage
import org.tasks.dialogs.DialogBuilder
import org.tasks.injection.InjectingAppCompatActivity
import org.tasks.location.LocationPickerAdapter.OnLocationPicked
import org.tasks.location.LocationSearchAdapter.OnPredictionPicked
import org.tasks.location.MapFragment.MapFragmentCallback
import org.tasks.preferences.ActivityPermissionRequestor
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.PermissionRequestor
import org.tasks.themes.ColorProvider
import org.tasks.themes.Theme
import org.tasks.ui.Toaster
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class LocationPickerActivity : InjectingAppCompatActivity(), Toolbar.OnMenuItemClickListener, MapFragmentCallback, OnLocationPicked, SearchView.OnQueryTextListener, OnPredictionPicked, MenuItem.OnActionExpandListener {
    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar

    @BindView(R.id.app_bar_layout)
    lateinit var appBarLayout: AppBarLayout

    @BindView(R.id.collapsing_toolbar_layout)
    lateinit var toolbarLayout: CollapsingToolbarLayout

    @BindView(R.id.coordinator)
    lateinit var coordinatorLayout: CoordinatorLayout

    @BindView(R.id.search)
    lateinit var searchView: View

    @BindView(R.id.loading_indicator)
    lateinit var loadingIndicator: ContentLoadingProgressBar

    @BindView(R.id.choose_recent_location)
    lateinit var chooseRecentLocation: View

    @BindView(R.id.recent_locations)
    lateinit var recyclerView: RecyclerView

    @Inject lateinit var theme: Theme
    @Inject lateinit var toaster: Toaster
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var permissionChecker: PermissionChecker
    @Inject lateinit var permissionRequestor: ActivityPermissionRequestor
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var map: MapFragment
    @Inject lateinit var geocoder: Geocoder
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var locationProvider: LocationProvider
    
    private var disposables: CompositeDisposable? = null
    private var mapPosition: MapPosition? = null
    private var recentsAdapter: LocationPickerAdapter? = null
    private var searchAdapter: LocationSearchAdapter? = null
    private var places: List<PlaceUsage> = emptyList()
    private var offset = 0
    private lateinit var search: MenuItem
    private val searchSubject = PublishSubject.create<String>()
    private val viewModel: PlaceSearchViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.applyTheme(this)
        setContentView(R.layout.activity_location_picker)
        ButterKnife.bind(this)
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
        (search.actionView as SearchView).setOnQueryTextListener(this)
        toolbar.setOnMenuItemClickListener(this)
        val themeColor = theme.themeColor
        themeColor.applyToStatusBarIcons(this)
        themeColor.applyToNavigationBar(this)
        themeColor.setStatusBarColor(toolbarLayout)
        themeColor.apply(toolbar)
        val dark = theme.themeBase.isDarkTheme(this)
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
        appBarLayout.addOnOffsetChangedListener(
                OnOffsetChangedListener { appBarLayout: AppBarLayout, offset: Int ->
                    if (offset == 0 && this.offset != 0) {
                        closeSearch()
                        AndroidUtilities.hideKeyboard(this)
                    }
                    this.offset = offset
                    toolbar.alpha = abs(offset / appBarLayout.totalScrollRange.toFloat())
                })
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

    @OnClick(R.id.current_location)
    fun onClick() = moveToCurrentLocation(true)

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

    @OnClick(R.id.select_this_location)
    fun selectLocation() {
        val mapPosition = map.mapPosition ?: return
        loadingIndicator.visibility = View.VISIBLE
        lifecycleScope.launch {
            val place = geocoder.reverseGeocode(mapPosition)
            loadingIndicator.visibility = View.GONE
            returnPlace(place)
        }
    }

    @OnClick(R.id.search)
    fun searchPlace() {
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
                locationProvider.currentLocation()?.let { map.movePosition(it, animate) }
            } catch (e: Exception) {
                toaster.longToast(e.message)
            }
        }
    }

    private fun returnPlace(place: Place?) {
        if (place == null) {
            Timber.e("Place is null")
            return
        }
        AndroidUtilities.hideKeyboard(this)
        lifecycleScope.launch {
            var place = place
            if (place.id <= 0) {
                val existing = locationDao.findPlace(
                        place.latitude.toLikeString(),
                        place.longitude.toLikeString())
                if (existing == null) {
                    place.id = locationDao.insert(place)
                } else {
                    place = existing
                }
            }
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_PLACE, place as Parcelable?))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        viewModel.observe(this, Observer { list: List<PlaceSearchResult?>? -> searchAdapter!!.submitList(list) }, Observer { place: Place? -> returnPlace(place) }, Observer { error: Event<String> -> handleError(error) })
        disposables = CompositeDisposable(
                searchSubject
                        .debounce(SEARCH_DEBOUNCE_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { query: String? -> viewModel.query(query, mapPosition) })
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDestroy()
    }

    private fun handleError(error: Event<String>) {
        val message = error.ifUnhandled
        if (!isNullOrEmpty(message)) {
            toaster.longToast(message)
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
        disposables!!.dispose()
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
        searchSubject.onNext(query)
        return true
    }

    override fun picked(prediction: PlaceSearchResult) {
        viewModel.fetch(prediction)
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        searchAdapter!!.submitList(emptyList())
        recyclerView.adapter = searchAdapter
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
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
        private const val SEARCH_DEBOUNCE_TIMEOUT = 300
    }
}