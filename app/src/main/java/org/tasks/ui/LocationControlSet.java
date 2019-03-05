package org.tasks.ui;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.tasks.PermissionUtil.verifyPermissions;
import static org.tasks.dialogs.LocationDialog.newLocationDialog;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.common.base.Strings;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.Location;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.LocationDialog;
import org.tasks.injection.FragmentComponent;
import org.tasks.location.GeofenceService;
import org.tasks.location.PlacePicker;
import org.tasks.preferences.FragmentPermissionRequestor;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class LocationControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_locations_pref;

  private static final int REQUEST_LOCATION_REMINDER = 12153;
  private static final int REQUEST_LOCATION_DETAILS = 12154;

  private static final String FRAG_TAG_LOCATION_DIALOG = "location_dialog";

  private static final String EXTRA_GEOFENCES = "extra_geofences";
  private final Set<Location> locations = new LinkedHashSet<>();
  @Inject GeofenceService geofenceService;
  @Inject FragmentPermissionRequestor permissionRequestor;
  @Inject Preferences preferences;
  @Inject DialogBuilder dialogBuilder;

  @BindView(R.id.alert_container)
  LinearLayout alertContainer;

  @BindView(R.id.alarms_add)
  View addLocation;

  private long taskId;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    taskId = task.getId();
    if (savedInstanceState == null) {
      locations.addAll(geofenceService.getGeofences(taskId));
    } else {
      List<Parcelable> geofenceArray = savedInstanceState.getParcelableArrayList(EXTRA_GEOFENCES);
      for (Parcelable geofence : geofenceArray) {
        locations.add((Location) geofence);
      }
    }
    setup(locations);

    return view;
  }

  @OnClick(R.id.alarms_add)
  void addAlarm(View view) {
    if (permissionRequestor.requestFineLocation()) {
      pickLocation();
    }
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_locations;
  }

  @Override
  public int getIcon() {
    return R.drawable.ic_outline_place_24px;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  private void setup(Collection<Location> locations) {
    if (locations.isEmpty()) {
      alertContainer.setVisibility(View.GONE);
      addLocation.setVisibility(View.VISIBLE);
    } else {
      addLocation.setVisibility(View.GONE);
      alertContainer.setVisibility(View.VISIBLE);
      alertContainer.removeAllViews();
      for (Location location : locations) {
        addGeolocationReminder(location);
      }
    }
  }

  @Override
  public boolean hasChanges(Task original) {
    return !newHashSet(geofenceService.getGeofences(taskId)).equals(locations);
  }

  @Override
  public void apply(Task task) {
    if (geofenceService.synchronizeGeofences(task.getId(), locations)) {
      task.setModificationDate(DateUtilities.now());
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelableArrayList(EXTRA_GEOFENCES, newArrayList(locations));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_LOCATION_REMINDER) {
      if (resultCode == Activity.RESULT_OK) {
        locations.clear();
        locations.add(PlacePicker.getPlace(data, preferences));
        setup(locations);
      }
    } else if (requestCode == REQUEST_LOCATION_DETAILS) {
      if (resultCode == Activity.RESULT_OK) {
        Location original = data.getParcelableExtra(LocationDialog.EXTRA_ORIGINAL);
        Location location = data.getParcelableExtra(LocationDialog.EXTRA_LOCATION);
        Timber.d("original: %s, updated: %s", original, location);
        locations.remove(original);
        if (location != null) {
          locations.add(location);
        }
        setup(locations);
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_LOCATION) {
      if (verifyPermissions(grantResults)) {
        pickLocation();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  private void pickLocation() {
    Intent intent = PlacePicker.getIntent(getActivity());
    if (intent != null) {
      startActivityForResult(intent, REQUEST_LOCATION_REMINDER);
    }
  }

  private void addGeolocationReminder(final Location location) {
    final View alertItem = getActivity().getLayoutInflater().inflate(R.layout.location_row, null);
    alertContainer.addView(alertItem);
    String name = location.getDisplayName();
    String address = location.getAddress();
    if (!Strings.isNullOrEmpty(address) && !address.equals(name)) {
      TextView addressView = alertItem.findViewById(R.id.location_address);
      addressView.setText(address);
      addressView.setVisibility(View.VISIBLE);
    }
    SpannableString spannableString = new SpannableString(name);
    spannableString.setSpan(
        new ClickableSpan() {
          @Override
          public void onClick(@NonNull View view) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(location.getGeoUri()));
            startActivity(intent);
          }
        },
        0,
        name.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    TextView nameView = alertItem.findViewById(R.id.location_name);
    nameView.setText(spannableString);
    nameView.setMovementMethod(LinkMovementMethod.getInstance());

    alertItem
        .findViewById(R.id.location_more)
        .setOnClickListener(
            v -> {
              LocationDialog dialog = newLocationDialog(location);
              dialog.setTargetFragment(this, REQUEST_LOCATION_DETAILS);
              dialog.show(getFragmentManager(), FRAG_TAG_LOCATION_DIALOG);
            });
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }
}
