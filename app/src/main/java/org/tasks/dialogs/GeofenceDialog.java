package org.tasks.dialogs;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.tasks.R;
import org.tasks.data.LocationExtensionsKt;
import org.tasks.data.entity.Geofence;
import org.tasks.data.Location;
import org.tasks.databinding.LocationDetailsBinding;
import org.tasks.preferences.PermissionChecker;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GeofenceDialog extends DialogFragment {

  public static final String EXTRA_GEOFENCE = "extra_geofence";
  private static final String EXTRA_ORIGINAL = "extra_original";

  @Inject DialogBuilder dialogBuilder;
  @Inject Activity context;
  @Inject PermissionChecker permissionChecker;

  private SwitchMaterial arrivalView;
  private SwitchMaterial departureView;

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
            ? original.getGeofence()
            : savedInstanceState.getParcelable(EXTRA_GEOFENCE);

    LayoutInflater layoutInflater = LayoutInflater.from(context);
    LocationDetailsBinding binding = LocationDetailsBinding.inflate(layoutInflater);
    arrivalView = binding.locationArrival;
    departureView = binding.locationDeparture;
    arrivalView.setChecked(geofence.isArrival());
    departureView.setChecked(geofence.isDeparture());
    return dialogBuilder
        .newDialog(LocationExtensionsKt.getDisplayName(original))
        .setView(binding.getRoot())
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
    return new Geofence(
            0L,
            0L,
            null,
            arrivalView.isChecked(),
            departureView.isChecked()
    );
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
