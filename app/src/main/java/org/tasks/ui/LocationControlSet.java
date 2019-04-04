package org.tasks.ui;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static org.tasks.PermissionUtil.verifyPermissions;
import static org.tasks.dialogs.GeofenceDialog.newGeofenceDialog;
import static org.tasks.location.LocationPickerActivity.EXTRA_PLACE;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.common.base.Strings;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.Geofence;
import org.tasks.data.Location;
import org.tasks.data.LocationDao;
import org.tasks.data.Place;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.GeofenceDialog;
import org.tasks.injection.FragmentComponent;
import org.tasks.location.GeofenceApi;
import org.tasks.location.LocationPickerActivity;
import org.tasks.preferences.Device;
import org.tasks.preferences.FragmentPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;

public class LocationControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_locations_pref;
  private static final int REQUEST_LOCATION_REMINDER = 12153;
  private static final int REQUEST_GEOFENCE_DETAILS = 12154;
  private static final String FRAG_TAG_LOCATION_DIALOG = "location_dialog";
  private static final String EXTRA_ORIGINAL = "extra_original_location";
  private static final String EXTRA_LOCATION = "extra_new_location";

  @Inject Preferences preferences;
  @Inject DialogBuilder dialogBuilder;
  @Inject GeofenceApi geofenceApi;
  @Inject LocationDao locationDao;
  @Inject Device device;
  @Inject FragmentPermissionRequestor permissionRequestor;
  @Inject PermissionChecker permissionChecker;

  @BindView(R.id.location_name)
  TextView locationName;

  @BindView(R.id.location_address)
  TextView locationAddress;

  @BindView(R.id.geofence_options)
  ImageView geofenceOptions;

  private Location original;
  private Location location;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    if (savedInstanceState == null) {
      if (!task.isNew()) {
        original = locationDao.getGeofences(task.getId());
        if (original != null) {
          location = new Location(original.geofence, original.place);
        }
      }
    } else {
      original = savedInstanceState.getParcelable(EXTRA_ORIGINAL);
      location = savedInstanceState.getParcelable(EXTRA_LOCATION);
    }

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();

    updateUi();
  }

  private void setLocation(@Nullable Location location) {
    this.location = location;
    updateUi();
  }

  private void updateUi() {
    if (this.location == null) {
      locationName.setText("");
      geofenceOptions.setVisibility(View.GONE);
      locationAddress.setVisibility(View.GONE);
    } else {
      geofenceOptions.setVisibility(device.supportsGeofences() ? View.VISIBLE : View.GONE);
      geofenceOptions.setImageResource(
          permissionChecker.canAccessLocation()
                  && (this.location.isArrival() || this.location.isDeparture())
              ? R.drawable.ic_outline_notifications_24px
              : R.drawable.ic_outline_notifications_off_24px);
      String name = this.location.getDisplayName();
      String address = this.location.getDisplayAddress();
      if (!Strings.isNullOrEmpty(address) && !address.equals(name)) {
        locationAddress.setText(address);
        locationAddress.setVisibility(View.VISIBLE);
      } else {
        locationAddress.setVisibility(View.GONE);
      }
      SpannableString spannableString = new SpannableString(name);
      spannableString.setSpan(
          new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {}
          },
          0,
          name.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      locationName.setText(spannableString);
    }
  }

  @OnClick({R.id.location_name, R.id.location_address})
  void locationClick(View view) {
    if (location == null) {
      chooseLocation();
    } else {
      List<Pair<Integer, Runnable>> options = new ArrayList<>();
      options.add(Pair.create(R.string.open_map, this::openMap));
      if (!Strings.isNullOrEmpty(location.getPhone())) {
        options.add(Pair.create(R.string.action_call, this::call));
      }
      if (!Strings.isNullOrEmpty(location.getUrl())) {
        options.add(Pair.create(R.string.visit_website, this::openWebsite));
      }
      options.add(Pair.create(R.string.choose_new_location, this::chooseLocation));
      options.add(Pair.create(R.string.delete, () -> setLocation(null)));
      dialogBuilder
          .newDialog()
          .setTitle(location.getDisplayName())
          .setItems(
              newArrayList(transform(options, o -> getString(o.first))),
              (dialog, which) -> options.get(which).second.run())
          .show();
    }
  }

  private void chooseLocation() {
    Intent intent = new Intent(getActivity(), LocationPickerActivity.class);
    if (location != null) {
      intent.putExtra(EXTRA_PLACE, (Parcelable) location.place);
    }
    startActivityForResult(intent, REQUEST_LOCATION_REMINDER);
  }

  @OnClick(R.id.geofence_options)
  void geofenceOptions(View view) {
    if (permissionRequestor.requestFineLocation()) {
      showGeofenceOptions();
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_LOCATION) {
      if (verifyPermissions(grantResults)) {
        showGeofenceOptions();
      } else {
        dialogBuilder
            .newMessageDialog(R.string.location_permission_required_geofence)
            .setTitle(R.string.missing_permissions)
            .setPositiveButton(android.R.string.ok, null)
            .show();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  private void showGeofenceOptions() {
    GeofenceDialog dialog = newGeofenceDialog(location);
    dialog.setTargetFragment(this, REQUEST_GEOFENCE_DETAILS);
    dialog.show(getFragmentManager(), FRAG_TAG_LOCATION_DIALOG);
  }

  @Override
  protected int getLayout() {
    return R.layout.location_row;
  }

  @Override
  public int getIcon() {
    return R.drawable.ic_outline_place_24px;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  private void openMap() {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(location.getGeoUri()));
    startActivity(intent);
  }

  private void openWebsite() {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(location.getUrl())));
  }

  private void call() {
    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + location.getPhone())));
  }

  @Override
  public boolean hasChanges(Task task) {
    if (original == null) {
      return location != null;
    }
    if (location == null) {
      return true;
    }
    if (!original.place.equals(location.place)) {
      return true;
    }
    return original.isDeparture() != location.isDeparture()
        || original.isArrival() != location.isArrival()
        || original.getRadius() != location.getRadius();
  }

  @Override
  public void apply(Task task) {
    if (original != null) {
      geofenceApi.cancel(original);
      locationDao.delete(original.geofence);
    }
    if (location != null) {
      Place place = location.place;
      Geofence geofence = location.geofence;
      geofence.setTask(task.getId());
      geofence.setPlace(place.getUid());
      geofence.setId(locationDao.insert(geofence));
      geofenceApi.register(Collections.singletonList(location));
    }
    task.setModificationDate(DateUtilities.now());
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelable(EXTRA_ORIGINAL, original);
    outState.putParcelable(EXTRA_LOCATION, location);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_LOCATION_REMINDER) {
      if (resultCode == Activity.RESULT_OK) {
        Place place = data.getParcelableExtra(EXTRA_PLACE);
        Geofence geofence;
        if (location == null) {
          int defaultReminders =
              preferences.getIntegerFromString(R.string.p_default_location_reminder_key, 1);
          geofence =
              new Geofence(
                  place.getUid(),
                  defaultReminders == 1 || defaultReminders == 3,
                  defaultReminders == 2 || defaultReminders == 3,
                  preferences.getInt(R.string.p_default_location_radius, 250));
        } else {
          Geofence existing = location.geofence;
          geofence =
              new Geofence(
                  place.getUid(),
                  existing.isArrival(),
                  existing.isDeparture(),
                  existing.getRadius());
        }
        setLocation(new Location(geofence, place));
      }
    } else if (requestCode == REQUEST_GEOFENCE_DETAILS) {
      if (resultCode == Activity.RESULT_OK) {
        location.geofence = data.getParcelableExtra(GeofenceDialog.EXTRA_GEOFENCE);
        updateUi();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }
}
