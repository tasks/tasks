package org.tasks.ui;

import static org.tasks.dialogs.LocationDialog.newLocationDialog;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.common.base.Strings;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import java.util.Collections;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.Geofence;
import org.tasks.data.Location;
import org.tasks.data.LocationDao;
import org.tasks.data.Place;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.LocationDialog;
import org.tasks.injection.FragmentComponent;
import org.tasks.location.GeofenceApi;
import org.tasks.location.PlacePicker;
import org.tasks.preferences.Preferences;

public class LocationControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_locations_pref;
  private static final int REQUEST_LOCATION_REMINDER = 12153;
  private static final int REQUEST_LOCATION_DETAILS = 12154;
  private static final String FRAG_TAG_LOCATION_DIALOG = "location_dialog";
  private static final String EXTRA_ORIGINAL = "extra_original_location";
  private static final String EXTRA_LOCATION = "extra_new_location";

  @Inject Preferences preferences;
  @Inject DialogBuilder dialogBuilder;
  @Inject GeofenceApi geofenceApi;
  @Inject LocationDao locationDao;

  @BindView(R.id.location_name)
  TextView locationName;

  @BindView(R.id.location_address)
  TextView locationAddress;

  @BindView(R.id.location_more)
  View locationOptions;

  private long taskId;
  private Location original;
  private Location location;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    taskId = task.getId();

    if (savedInstanceState == null) {
      original = locationDao.getGeofences(taskId);
      location = original;
    } else {
      original = savedInstanceState.getParcelable(EXTRA_ORIGINAL);
      location = savedInstanceState.getParcelable(EXTRA_LOCATION);
    }

    updateUI();

    return view;
  }

  @OnClick({R.id.location_name, R.id.location_address})
  void addAlarm(View view) {
    if (location == null) {
      startActivityForResult(PlacePicker.getIntent(getActivity()), REQUEST_LOCATION_REMINDER);
    } else {
      openMap();
    }
  }

  @OnClick(R.id.location_more)
  void locationOptions(View view) {
    LocationDialog dialog = newLocationDialog(location);
    dialog.setTargetFragment(this, REQUEST_LOCATION_DETAILS);
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

  private void updateUI() {
    if (location == null) {
      locationName.setText("");
      locationOptions.setVisibility(View.GONE);
      locationAddress.setVisibility(View.GONE);
    } else {
      locationOptions.setVisibility(View.VISIBLE);
      String name = location.getDisplayName();
      String address = location.getAddress();
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
            public void onClick(@NonNull View view) {
              openMap();
            }
          },
          0,
          name.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      locationName.setText(spannableString);
      locationName.setMovementMethod(LinkMovementMethod.getInstance());
    }
  }

  private void openMap() {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(location.getGeoUri()));
    startActivity(intent);
  }

  @Override
  public boolean hasChanges(Task task) {
    return original == null ? location == null : !original.equals(location);
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
      geofence.setTask(taskId);
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
        location = PlacePicker.getPlace(data, preferences);
        updateUI();
      }
    } else if (requestCode == REQUEST_LOCATION_DETAILS) {
      if (resultCode == Activity.RESULT_OK) {
        location = data.getParcelableExtra(LocationDialog.EXTRA_LOCATION);
        updateUI();
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
