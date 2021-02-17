package org.tasks.dialogs;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.Geofence;
import org.tasks.data.Location;
import org.tasks.locale.Locale;
import org.tasks.preferences.PermissionChecker;
import org.tasks.ui.Toaster;

@AndroidEntryPoint
public class GeofenceDialog extends DialogFragment {

  public static final String EXTRA_GEOFENCE = "extra_geofence";
  private static final String EXTRA_ORIGINAL = "extra_original";
  private static final int MIN_RADIUS = 75;
  private static final int MAX_RADIUS = 1000;
  private static final int STEP = 25;

  @Inject DialogBuilder dialogBuilder;
  @Inject Activity context;
  @Inject Locale locale;
  @Inject PermissionChecker permissionChecker;
  @Inject Toaster toaster;

  @BindView(R.id.location_arrival)
  SwitchMaterial arrivalView;

  @BindView(R.id.location_departure)
  SwitchMaterial departureView;

  @BindView(R.id.slider)
  Slider slider;

  public static GeofenceDialog newGeofenceDialog(Location location) {
    GeofenceDialog dialog = new GeofenceDialog();
    Bundle args = new Bundle();
    args.putParcelable(EXTRA_ORIGINAL, location);
    dialog.setArguments(args);
    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Location original = getArguments().getParcelable(EXTRA_ORIGINAL);
    Geofence geofence =
        savedInstanceState == null
            ? original.geofence
            : savedInstanceState.getParcelable(EXTRA_GEOFENCE);

    LayoutInflater layoutInflater = LayoutInflater.from(context);
    View view = layoutInflater.inflate(R.layout.location_details, null);
    ButterKnife.bind(this, view);
    arrivalView.setChecked(geofence.isArrival());
    departureView.setChecked(geofence.isDeparture());
    slider.setLabelFormatter(
        value -> getString(R.string.location_radius_meters, locale.formatNumber(value)));
    slider.setValueTo(MAX_RADIUS);
    slider.setValueFrom(MIN_RADIUS);
    slider.setStepSize(STEP);
    slider.setHaloRadius(0);
    slider.setValue(Math.round((geofence.getRadius() / STEP) * STEP));
    return dialogBuilder
        .newDialog(original.getDisplayName())
        .setView(view)
        .setNegativeButton(R.string.cancel, null)
        .setOnCancelListener(this::sendResult)
        .setPositiveButton(R.string.ok, this::sendResult)
        .create();
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    sendResult(dialog);
  }

  private Geofence toGeofence() {
    Geofence geofence = new Geofence();
    geofence.setArrival(arrivalView.isChecked());
    geofence.setDeparture(departureView.isChecked());
    geofence.setRadius((int) slider.getValue());
    return geofence;
  }

  private void sendResult(DialogInterface d, int... i) {
    Intent data = new Intent();
    data.putExtra(EXTRA_GEOFENCE, (Parcelable) toGeofence());
    getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, data);
    dismiss();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelable(EXTRA_GEOFENCE, toGeofence());
  }

  @Override
  public void onResume() {
    super.onResume();

    if (!permissionChecker.canAccessBackgroundLocation()) {
      dismiss();
    }
  }
}
