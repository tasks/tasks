package org.tasks.location;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static java.util.Arrays.asList;
import static org.tasks.data.Place.newPlace;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.Place.Field;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.AppBarLayout.Behavior;
import com.google.common.base.Strings;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.data.LocationDao;
import org.tasks.data.PlaceUsage;
import org.tasks.gtasks.PlayServices;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.location.LocationPickerAdapter.OnLocationPicked;
import org.tasks.location.MapFragment.MapFragmentCallback;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.MenuColorizer;
import org.tasks.ui.Toaster;
import timber.log.Timber;

public class LocationPicker extends InjectingAppCompatActivity
    implements OnMenuItemClickListener, MapFragmentCallback, OnLocationPicked {

  private static final String EXTRA_MAP_POSITION = "extra_map_position";
  private static final String EXTRA_APPBAR_OFFSET = "extra_appbar_offset";
  private static final String FRAG_TAG_MAP = "frag_tag_map";
  private static final int REQUEST_GOOGLE_AUTOCOMPLETE = 10101;
  private static final int REQUEST_MAPBOX_AUTOCOMPLETE = 10102;
  private static final Pattern pattern = Pattern.compile("(\\d+):(\\d+):(\\d+\\.\\d+)");

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
  RecyclerView recentLocations;

  @Inject @ForApplication Context context;
  @Inject Theme theme;
  @Inject Toaster toaster;
  @Inject Inventory inventory;
  @Inject PlayServices playServices;
  @Inject Preferences preferences;
  @Inject LocationDao locationDao;

  private MapFragment map;
  private FusedLocationProviderClient fusedLocationProviderClient;
  private CompositeDisposable disposables;
  private MapPosition mapPosition;
  private LocationPickerAdapter adapter = new LocationPickerAdapter(this);
  private List<PlaceUsage> places = Collections.emptyList();
  private int offset;

  private static String formatCoordinates(org.tasks.data.Place place) {
    return String.format(
        "%s %s",
        formatCoordinate(place.getLatitude(), true), formatCoordinate(place.getLongitude(), false));
  }

  private static String formatCoordinate(double coordinates, boolean latitude) {
    String output = Location.convert(Math.abs(coordinates), Location.FORMAT_SECONDS);
    Matcher matcher = pattern.matcher(output);
    if (matcher.matches()) {
      return String.format(
          "%s°%s'%s\"%s",
          matcher.group(1),
          matcher.group(2),
          matcher.group(3),
          latitude ? (coordinates > 0 ? "N" : "S") : (coordinates > 0 ? "E" : "W"));
    } else {
      return Double.toString(coordinates);
    }
  }

  private boolean canSearch() {
    return atLeastLollipop() || inventory.hasPro();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    theme.applyTheme(this);
    setContentView(R.layout.activity_location_picker);
    ButterKnife.bind(this);

    Configuration configuration = getResources().getConfiguration();
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        && configuration.smallestScreenWidthDp < 480) {
      searchView.setVisibility(View.GONE);
    }

    if (savedInstanceState != null) {
      mapPosition = savedInstanceState.getParcelable(EXTRA_MAP_POSITION);
      offset = savedInstanceState.getInt(EXTRA_APPBAR_OFFSET);
    }

    toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24px);
    toolbar.setNavigationOnClickListener(v -> collapseToolbar());
    if (canSearch()) {
      toolbar.inflateMenu(R.menu.menu_location_picker);
      toolbar.setOnMenuItemClickListener(this);
    } else {
      searchView.setVisibility(View.GONE);
    }

    MenuColorizer.colorToolbar(this, toolbar);
    ThemeColor themeColor = theme.getThemeColor();
    themeColor.applyToStatusBarIcons(this);
    themeColor.applyToNavigationBar(this);

    if (preferences.useGoogleMaps()) {
      initGoogleMaps();
    } else {
      initMapboxMaps();
    }

    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

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
          this.offset = offset;
          toolbar.setAlpha(Math.abs(offset / (float) appBarLayout.getTotalScrollRange()));
        });

    coordinatorLayout.addOnLayoutChangeListener(
        new OnLayoutChangeListener() {
          @Override
          public void onLayoutChange(
              View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
            coordinatorLayout.removeOnLayoutChangeListener(this);
            updateAppbarLayout();
          }
        });

    if (offset != 0) {
      appBarLayout.post(this::expandToolbar);
    }

    adapter.setHasStableIds(true);
    ((DefaultItemAnimator) recentLocations.getItemAnimator()).setSupportsChangeAnimations(false);
    recentLocations.setLayoutManager(new LinearLayoutManager(this));
    recentLocations.setAdapter(adapter);
  }

  private void initGoogleMaps() {
    FragmentManager supportFragmentManager = getSupportFragmentManager();
    SupportMapFragment mapFragment =
        (SupportMapFragment) supportFragmentManager.findFragmentByTag(FRAG_TAG_MAP);
    if (mapFragment == null) {
      mapFragment = new SupportMapFragment();
      supportFragmentManager.beginTransaction().replace(R.id.map, mapFragment).commit();
    }
    new GoogleMapFragment(context, mapFragment, this, theme.getThemeBase().isDarkTheme(this));
  }

  private void initMapboxMaps() {
    Mapbox.getInstance(this, getString(R.string.mapbox_key));

    FragmentManager supportFragmentManager = getSupportFragmentManager();
    com.mapbox.mapboxsdk.maps.SupportMapFragment mapFragment =
        (com.mapbox.mapboxsdk.maps.SupportMapFragment)
            supportFragmentManager.findFragmentByTag(FRAG_TAG_MAP);
    if (mapFragment == null) {
      mapFragment = new com.mapbox.mapboxsdk.maps.SupportMapFragment();
      supportFragmentManager.beginTransaction().replace(R.id.map, mapFragment).commit();
    }
    new MapboxMapFragment(context, mapFragment, this, theme.getThemeBase().isDarkTheme(this));
  }

  @Override
  public void onMapReady(MapFragment mapFragment) {
    map = mapFragment;
    if (mapPosition != null) {
      map.movePosition(mapPosition, false);
    } else {
      moveToCurrentLocation(false);
    }
    updateMarkers();
  }

  @Override
  public void onPlaceSelected(org.tasks.data.Place place) {
    returnPlace(place);
  }

  @OnClick(R.id.current_location)
  void onClick() {
    moveToCurrentLocation(true);
  }

  @OnClick(R.id.select_this_location)
  void selectLocation() {
    loadingIndicator.setVisibility(View.VISIBLE);

    MapPosition mapPosition = map.getMapPosition();
    disposables.add(
        Single.fromCallable(
                () -> {
                  Geocoder geocoder = new Geocoder(this);
                  return geocoder.getFromLocation(
                      mapPosition.getLatitude(), mapPosition.getLongitude(), 1);
                })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError(e -> toaster.longToast(e.getMessage()))
            .doFinally(() -> loadingIndicator.setVisibility(View.GONE))
            .subscribe(
                addresses -> {
                  org.tasks.data.Place place = newPlace();
                  if (addresses.isEmpty()) {
                    place.setLatitude(mapPosition.getLatitude());
                    place.setLongitude(mapPosition.getLongitude());
                  } else {
                    Address address = addresses.get(0);
                    place.setLatitude(address.getLatitude());
                    place.setLongitude(address.getLongitude());
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                      stringBuilder.append(address.getAddressLine(i)).append("\n");
                    }
                    place.setPhone(address.getPhone());
                    place.setAddress(stringBuilder.toString().trim());
                    String url = address.getUrl();
                    if (!Strings.isNullOrEmpty(url)) {
                      place.setUrl(url);
                    }
                  }
                  place.setName(formatCoordinates(place));
                  returnPlace(place);
                }));
  }

  @OnClick(R.id.search)
  void searchPlace() {
    if (preferences.useGooglePlaces() && inventory.hasPro()) {
      if (!Places.isInitialized()) {
        Places.initialize(this, getString(R.string.google_key));
      }

      startActivityForResult(
          new Autocomplete.IntentBuilder(
                  AutocompleteActivityMode.FULLSCREEN,
                  asList(
                      Field.ID,
                      Field.LAT_LNG,
                      Field.ADDRESS,
                      Field.WEBSITE_URI,
                      Field.NAME,
                      Field.PHONE_NUMBER))
              .build(this),
          REQUEST_GOOGLE_AUTOCOMPLETE);
    } else {
      String token = getString(R.string.mapbox_key);
      Mapbox.getInstance(this, token);
      MapPosition mapPosition = map.getMapPosition();
      startActivityForResult(
          new PlaceAutocomplete.IntentBuilder()
              .accessToken(token)
              .placeOptions(
                  PlaceOptions.builder()
                      .backgroundColor(getResources().getColor(R.color.white_100))
                      .proximity(
                          Point.fromLngLat(mapPosition.getLongitude(), mapPosition.getLatitude()))
                      .build())
              .build(this),
          REQUEST_MAPBOX_AUTOCOMPLETE);
    }
  }

  @SuppressLint("MissingPermission")
  private void moveToCurrentLocation(boolean animate) {
    fusedLocationProviderClient
        .getLastLocation()
        .addOnSuccessListener(
            location -> {
              if (location != null) {
                map.movePosition(
                    new MapPosition(location.getLatitude(), location.getLongitude(), 15f), animate);
              }
            });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == REQUEST_GOOGLE_AUTOCOMPLETE) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        returnPlace(Autocomplete.getPlaceFromIntent(data));
      } else if (resultCode == AutocompleteActivity.RESULT_ERROR && data != null) {
        Status status = Autocomplete.getStatusFromIntent(data);
        toaster.longToast(status.getStatusMessage());
      }
    } else if (requestCode == REQUEST_MAPBOX_AUTOCOMPLETE) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        returnPlace(PlaceAutocomplete.getPlace(data));
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void returnPlace(CarmenFeature place) {
    org.tasks.data.Place result = newPlace();
    result.setName(place.placeName());
    result.setAddress(place.address());
    result.setLatitude(place.center().latitude());
    result.setLongitude(place.center().longitude());
    returnPlace(result);
  }

  private void returnPlace(Place place) {
    LatLng latLng = place.getLatLng();
    org.tasks.data.Place result = newPlace();
    result.setName(place.getName());
    CharSequence address = place.getAddress();
    if (address != null) {
      result.setAddress(place.getAddress());
    }
    CharSequence phoneNumber = place.getPhoneNumber();
    if (phoneNumber != null) {
      result.setPhone(phoneNumber.toString());
    }
    Uri uri = place.getWebsiteUri();
    if (uri != null) {
      result.setUrl(uri.toString());
    }
    result.setLatitude(latLng.latitude);
    result.setLongitude(latLng.longitude);
    returnPlace(result);
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
        long placeId = locationDao.insert(place);
        place.setId(placeId);
      } else {
        existing.apply(place);
        locationDao.update(existing);
        place = existing;
      }
    }
    setResult(RESULT_OK, new Intent().putExtra(PlacePicker.EXTRA_PLACE, (Parcelable) place));
    finish();
  }

  @Override
  protected void onResume() {
    super.onResume();

    disposables = new CompositeDisposable(playServices.checkMaps(this));

    locationDao.getPlaceUsage().observe(this, this::updatePlaces);
  }

  private void updatePlaces(List<PlaceUsage> places) {
    this.places = places;
    updateMarkers();
    adapter.submitList(places);
    updateAppbarLayout();
    if (places.isEmpty()) {
      collapseToolbar();
    }
  }

  private void updateMarkers() {
    if (map != null) {
      map.setMarkers(newArrayList(transform(places, PlaceUsage::getPlace)));
    }
  }

  private void updateAppbarLayout() {
    CoordinatorLayout.LayoutParams params =
        (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();

    if (places.isEmpty()) {
      params.height = coordinatorLayout.getHeight();
      chooseRecentLocation.setVisibility(View.GONE);
    } else {
      params.height = (coordinatorLayout.getHeight() * 75) / 100;
      chooseRecentLocation.setVisibility(View.VISIBLE);
    }
  }

  private void collapseToolbar() {
    appBarLayout.setExpanded(true, true);
  }

  private void expandToolbar() {
    appBarLayout.setExpanded(false, false);
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
}
