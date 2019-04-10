package org.tasks.location;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.hideKeyboard;
import static org.tasks.PermissionUtil.verifyPermissions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.AppBarLayout.Behavior;
import com.google.common.base.Strings;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineResult;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.tasks.Event;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.data.LocationDao;
import org.tasks.data.Place;
import org.tasks.data.PlaceUsage;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.location.LocationPickerAdapter.OnLocationPicked;
import org.tasks.location.LocationSearchAdapter.OnPredictionPicked;
import org.tasks.location.MapFragment.MapFragmentCallback;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.MenuColorizer;
import org.tasks.ui.Toaster;
import timber.log.Timber;

public class LocationPickerActivity extends InjectingAppCompatActivity
    implements OnMenuItemClickListener,
        MapFragmentCallback,
        OnLocationPicked,
        OnQueryTextListener,
        OnPredictionPicked,
        OnActionExpandListener {

  public static final String EXTRA_PLACE = "extra_place";
  private static final String EXTRA_MAP_POSITION = "extra_map_position";
  private static final String EXTRA_APPBAR_OFFSET = "extra_appbar_offset";
  private static final int SEARCH_DEBOUNCE_TIMEOUT = 300;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.app_bar_layout)
  AppBarLayout appBarLayout;

  @BindView(R.id.coordinator)
  CoordinatorLayout coordinatorLayout;

  @BindView(R.id.search)
  View searchView;

  @BindView(R.id.loading_indicator)
  ContentLoadingProgressBar loadingIndicator;

  @BindView(R.id.choose_recent_location)
  View chooseRecentLocation;

  @BindView(R.id.recent_locations)
  RecyclerView recyclerView;

  @Inject @ForApplication Context context;
  @Inject Theme theme;
  @Inject Toaster toaster;
  @Inject Inventory inventory;
  @Inject LocationDao locationDao;
  @Inject PlaceSearchProvider searchProvider;
  @Inject PermissionChecker permissionChecker;
  @Inject ActivityPermissionRequestor permissionRequestor;
  @Inject DialogBuilder dialogBuilder;
  @Inject MapFragment map;
  @Inject Geocoder geocoder;

  private CompositeDisposable disposables;
  @Nullable private MapPosition mapPosition;
  private LocationPickerAdapter recentsAdapter = new LocationPickerAdapter(this);
  private LocationSearchAdapter searchAdapter;
  private List<PlaceUsage> places = Collections.emptyList();
  private int offset;
  private MenuItem search;
  private PublishSubject<String> searchSubject = PublishSubject.create();
  private PlaceSearchViewModel viewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    theme.applyTheme(this);
    setContentView(R.layout.activity_location_picker);
    ButterKnife.bind(this);

    viewModel = ViewModelProviders.of(this).get(PlaceSearchViewModel.class);
    viewModel.setSearchProvider(searchProvider);

    Configuration configuration = getResources().getConfiguration();
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        && configuration.smallestScreenWidthDp < 480) {
      searchView.setVisibility(View.GONE);
    }

    Place currentPlace = getIntent().getParcelableExtra(EXTRA_PLACE);
    recentsAdapter.setCurrentPlace(currentPlace);

    if (savedInstanceState == null) {
      mapPosition =
          currentPlace == null
              ? getIntent().getParcelableExtra(EXTRA_MAP_POSITION)
              : currentPlace.getMapPosition();
    } else {
      mapPosition = savedInstanceState.getParcelable(EXTRA_MAP_POSITION);
      offset = savedInstanceState.getInt(EXTRA_APPBAR_OFFSET);
      viewModel.restoreState(savedInstanceState);
    }

    toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24px);
    toolbar.setNavigationOnClickListener(v -> collapseToolbar());
    toolbar.inflateMenu(R.menu.menu_location_picker);
    Menu menu = toolbar.getMenu();
    search = menu.findItem(R.id.menu_search);
    search.setOnActionExpandListener(this);
    ((SearchView) search.getActionView()).setOnQueryTextListener(this);
    toolbar.setOnMenuItemClickListener(this);

    MenuColorizer.colorToolbar(this, toolbar);
    ThemeColor themeColor = theme.getThemeColor();
    themeColor.applyToStatusBarIcons(this);
    themeColor.applyToNavigationBar(this);

    boolean dark = theme.getThemeBase().isDarkTheme(this);
    map.init(getSupportFragmentManager(), this, dark);

    CoordinatorLayout.LayoutParams params =
        (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
    Behavior behavior = new Behavior();
    behavior.setDragCallback(
        new AppBarLayout.Behavior.DragCallback() {
          @Override
          public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
            return false;
          }
        });
    params.setBehavior(behavior);

    appBarLayout.addOnOffsetChangedListener(
        (appBarLayout, offset) -> {
          if (offset == 0 && this.offset != 0) {
            closeSearch();
            hideKeyboard(this);
          }
          this.offset = offset;
          toolbar.setAlpha(Math.abs(offset / (float) appBarLayout.getTotalScrollRange()));
        });

    coordinatorLayout.addOnLayoutChangeListener(
        new OnLayoutChangeListener() {
          @Override
          public void onLayoutChange(
              View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
            coordinatorLayout.removeOnLayoutChangeListener(this);
            locationDao
                .getPlaceUsage()
                .observe(LocationPickerActivity.this, LocationPickerActivity.this::updatePlaces);
          }
        });

    if (offset != 0) {
      appBarLayout.post(() -> expandToolbar(false));
    }

    findViewById(map.getMarkerId()).setVisibility(View.VISIBLE);

    searchAdapter = new LocationSearchAdapter(searchProvider.getAttributionRes(dark), this);
    recentsAdapter.setHasStableIds(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(
        search != null && search.isActionViewExpanded() ? searchAdapter : recentsAdapter);
  }

  @Override
  public void onMapReady(MapFragment mapFragment) {
    map = mapFragment;
    updateMarkers();
    if (permissionChecker.canAccessLocation()) {
      mapFragment.showMyLocation();
    }
    if (mapPosition != null) {
      map.movePosition(mapPosition, false);
    } else if (permissionRequestor.requestFineLocation()) {
      moveToCurrentLocation(false);
    }
  }

  @Override
  public void onBackPressed() {
    if (closeSearch()) {
      return;
    }

    if (offset != 0) {
      collapseToolbar();
      return;
    }

    super.onBackPressed();
  }

  private boolean closeSearch() {
    return search != null && search.isActionViewExpanded() && search.collapseActionView();
  }

  @Override
  public void onPlaceSelected(org.tasks.data.Place place) {
    returnPlace(place);
  }

  @OnClick(R.id.current_location)
  void onClick() {
    if (permissionRequestor.requestFineLocation()) {
      moveToCurrentLocation(true);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_LOCATION) {
      if (verifyPermissions(grantResults)) {
        map.showMyLocation();
        moveToCurrentLocation(true);
      } else {
        dialogBuilder
            .newMessageDialog(R.string.location_permission_required_location)
            .setTitle(R.string.missing_permissions)
            .setPositiveButton(android.R.string.ok, null)
            .show();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @OnClick(R.id.select_this_location)
  void selectLocation() {
    loadingIndicator.setVisibility(View.VISIBLE);
    MapPosition mapPosition = map.getMapPosition();
    disposables.add(
        Single.fromCallable(() -> geocoder.reverseGeocode(mapPosition))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally(() -> loadingIndicator.setVisibility(View.GONE))
            .subscribe(this::returnPlace, e -> toaster.longToast(e.getMessage())));
  }

  @OnClick(R.id.search)
  void searchPlace() {
    mapPosition = map.getMapPosition();
    expandToolbar(true);
    search.expandActionView();
  }

  @SuppressLint("MissingPermission")
  private void moveToCurrentLocation(boolean animate) {
    LocationEngineProvider.getBestLocationEngine(this)
        .getLastLocation(
            new LocationEngineCallback<LocationEngineResult>() {
              @Override
              public void onSuccess(LocationEngineResult result) {
                Location location = result.getLastLocation();
                if (location != null) {
                  map.movePosition(
                      new MapPosition(location.getLatitude(), location.getLongitude()), animate);
                }
              }

              @Override
              public void onFailure(@NonNull Exception exception) {
                toaster.longToast(exception.getMessage());
              }
            });
  }

  private void returnPlace(org.tasks.data.Place place) {
    if (place == null) {
      Timber.e("Place is null");
      return;
    }
    if (place.getId() <= 0) {
      org.tasks.data.Place existing =
          locationDao.findPlace(place.getLatitude(), place.getLongitude());
      if (existing == null) {
        place.setId(locationDao.insert(place));
      } else {
        place = existing;
      }
    }
    hideKeyboard(this);
    setResult(RESULT_OK, new Intent().putExtra(EXTRA_PLACE, (Parcelable) place));
    finish();
  }

  @Override
  protected void onResume() {
    super.onResume();

    viewModel.observe(this, searchAdapter::submitList, this::returnPlace, this::handleError);

    disposables =
        new CompositeDisposable(
            searchSubject
                .debounce(SEARCH_DEBOUNCE_TIMEOUT, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(query -> viewModel.query(query, mapPosition)));
  }

  private void handleError(Event<String> error) {
    String message = error.getIfUnhandled();
    if (!Strings.isNullOrEmpty(message)) {
      toaster.longToast(message);
    }
  }

  private void updatePlaces(List<PlaceUsage> places) {
    this.places = places;
    updateMarkers();
    recentsAdapter.submitList(places);

    CoordinatorLayout.LayoutParams params =
        (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();

    int height = coordinatorLayout.getHeight();
    if (this.places.isEmpty()) {
      params.height = height;
      chooseRecentLocation.setVisibility(View.GONE);
      collapseToolbar();
    } else {
      params.height = (height * 75) / 100;
      chooseRecentLocation.setVisibility(View.VISIBLE);
    }
  }

  private void updateMarkers() {
    if (map != null) {
      map.setMarkers(newArrayList(transform(places, PlaceUsage::getPlace)));
    }
  }

  private void collapseToolbar() {
    appBarLayout.setExpanded(true, true);
  }

  private void expandToolbar(boolean animate) {
    appBarLayout.setExpanded(false, animate);
  }

  @Override
  protected void onPause() {
    super.onPause();

    disposables.dispose();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelable(EXTRA_MAP_POSITION, map.getMapPosition());
    outState.putInt(EXTRA_APPBAR_OFFSET, offset);
    viewModel.saveState(outState);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.menu_search) {
      searchPlace();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void picked(org.tasks.data.Place place) {
    returnPlace(place);
  }

  @Override
  public void delete(org.tasks.data.Place place) {
    locationDao.delete(place);
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    return false;
  }

  @Override
  public boolean onQueryTextChange(String query) {
    searchSubject.onNext(query);
    return true;
  }

  @Override
  public void picked(PlaceSearchResult prediction) {
    viewModel.fetch(prediction);
  }

  @Override
  public boolean onMenuItemActionExpand(MenuItem item) {
    searchAdapter.submitList(Collections.emptyList());
    recyclerView.setAdapter(searchAdapter);
    return true;
  }

  @Override
  public boolean onMenuItemActionCollapse(MenuItem item) {
    recyclerView.setAdapter(recentsAdapter);
    if (places.isEmpty()) {
      collapseToolbar();
    }
    return true;
  }
}
