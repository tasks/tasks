package org.tasks.ui;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static org.tasks.dialogs.LocationDialog.newLocationDialog;
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
import org.tasks.dialogs.LocationDialog;
import org.tasks.injection.FragmentComponent;
import org.tasks.location.GeofenceApi;
import org.tasks.location.LocationPickerActivity;
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
  ImageView locationOptions;

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
          setLocation(new Location(original.geofence, original.place));
        }
      }
    } else {
      original = savedInstanceState.getParcelable(EXTRA_ORIGINAL);
      setLocation(savedInstanceState.getParcelable(EXTRA_LOCATION));
    }

    return view;
  }

  private void setLocation(@Nullable Location location) {
    this.location = location;
    if (this.location == null) {
      locationName.setText("");
      locationOptions.setVisibility(View.GONE);
      locationAddress.setVisibility(View.GONE);
    } else {
      locationOptions.setVisibility(View.VISIBLE);
      locationOptions.setImageResource(
          this.location.isArrival() || this.location.isDeparture()
              ? R.drawable.ic_outline_notifications_24px
              : R.drawable.ic_outline_notifications_off_24px);
      String name = this.location.getDisplayName();
      String address = this.location.getAddress();
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
        Geofence geofence = new Geofence();
        if (location == null) {
          geofence.setRadius(preferences.getInt(R.string.p_default_location_radius, 250));
          int defaultReminders =
              preferences.getIntegerFromString(R.string.p_default_location_reminder_key, 1);
          geofence.setArrival(defaultReminders == 1 || defaultReminders == 3);
          geofence.setDeparture(defaultReminders == 2 || defaultReminders == 3);
        } else {
          Geofence existing = location.geofence;
          geofence.setArrival(existing.isArrival());
          geofence.setDeparture(existing.isDeparture());
          geofence.setRadius(existing.getRadius());
        }
        setLocation(new Location(geofence, place));
      }
    } else if (requestCode == REQUEST_LOCATION_DETAILS) {
      if (resultCode == Activity.RESULT_OK) {
        location.geofence = data.getParcelableExtra(LocationDialog.EXTRA_GEOFENCE);
        setLocation(location);
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
