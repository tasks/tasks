package org.tasks.dialogs;

import static android.app.Activity.RESULT_OK;
import static org.tasks.dialogs.SeekBarDialog.newSeekBarDialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.Geofence;
import org.tasks.data.Location;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.locale.Locale;
import org.tasks.preferences.PermissionChecker;
import org.tasks.ui.Toaster;

public class GeofenceDialog extends InjectingDialogFragment {

  public static final String EXTRA_GEOFENCE = "extra_geofence";
  private static final String EXTRA_ORIGINAL = "extra_original";

  private static final String FRAG_TAG_SEEKBAR = "frag_tag_seekbar";
  private static final int REQUEST_RADIUS = 10101;

  @Inject DialogBuilder dialogBuilder;
  @Inject @ForActivity Context context;
  @Inject Locale locale;
  @Inject PermissionChecker permissionChecker;
  @Inject Toaster toaster;

  @BindView(R.id.location_arrival)
  Switch arrivalView;

  @BindView(R.id.location_departure)
  Switch departureView;

  @BindView(R.id.location_radius_value)
  TextView radiusValue;

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
    boolean hasLocationPermission = permissionChecker.canAccessLocation();
    arrivalView.setChecked(hasLocationPermission && geofence.isArrival());
    departureView.setChecked(hasLocationPermission && geofence.isDeparture());
    updateRadius(geofence.getRadius());
    return dialogBuilder
        .newDialog()
        .setTitle(original.getDisplayName())
        .setView(view)
        .setNegativeButton(android.R.string.cancel, null)
        .setOnCancelListener(this::sendResult)
        .setPositiveButton(android.R.string.ok, this::sendResult)
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
    geofence.setRadius((int) radiusValue.getTag());
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

    if (!permissionChecker.canAccessLocation()) {
      dismiss();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_RADIUS) {
      if (resultCode == RESULT_OK) {
        updateRadius(data.getIntExtra(SeekBarDialog.EXTRA_VALUE, 250));
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @OnClick(R.id.location_radius)
  void selectRadius() {
    SeekBarDialog seekBarDialog =
        newSeekBarDialog(R.layout.dialog_radius_seekbar, 75, 1000, toGeofence().getRadius());
    seekBarDialog.setTargetFragment(this, REQUEST_RADIUS);
    seekBarDialog.show(getFragmentManager(), FRAG_TAG_SEEKBAR);
  }

  private void updateRadius(int radius) {
    radiusValue.setText(getString(R.string.location_radius_meters, locale.formatNumber(radius)));
    radiusValue.setTag(radius);
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }
}
