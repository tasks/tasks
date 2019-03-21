package org.tasks.dialogs;

import static android.app.Activity.RESULT_OK;
import static org.tasks.dialogs.SeekBarDialog.newSeekBarDialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
import com.google.common.base.Strings;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.Location;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.locale.Locale;

public class LocationDialog extends InjectingDialogFragment {

  public static final String EXTRA_LOCATION = "extra_location";
  public static final String EXTRA_ORIGINAL = "extra_original";

  private static final String FRAG_TAG_SEEKBAR = "frag_tag_seekbar";
  private static final int REQUEST_RADIUS = 10101;

  @Inject DialogBuilder dialogBuilder;
  @Inject @ForActivity Context context;
  @Inject Locale locale;

  @BindView(R.id.location_arrival)
  Switch arrivalView;

  @BindView(R.id.location_departure)
  Switch departureView;

  @BindView(R.id.location_call)
  TextView callView;

  @BindView(R.id.location_url)
  TextView urlView;

  @BindView(R.id.location_radius_value)
  TextView radiusValue;

  public static LocationDialog newLocationDialog(Location location) {
    LocationDialog dialog = new LocationDialog();
    Bundle args = new Bundle();
    args.putParcelable(EXTRA_ORIGINAL, location);
    dialog.setArguments(args);
    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Location location =
        savedInstanceState == null
            ? getOriginal()
            : savedInstanceState.getParcelable(EXTRA_LOCATION);

    LayoutInflater layoutInflater = LayoutInflater.from(context);
    View view = layoutInflater.inflate(R.layout.location_details, null);
    ButterKnife.bind(this, view);
    arrivalView.setChecked(location.isArrival());
    departureView.setChecked(location.isDeparture());
    updateRadius(location.getRadius());
    String phone = location.getPhone();
    if (!Strings.isNullOrEmpty(phone)) {
      callView.setVisibility(View.VISIBLE);
      callView.setText(getString(R.string.call_number, phone));
    }
    String url = location.getUrl();
    if (!Strings.isNullOrEmpty(url)) {
      urlView.setVisibility(View.VISIBLE);
      urlView.setText(getString(R.string.open_url, url));
    }
    return dialogBuilder
        .newDialog()
        .setTitle(location.getDisplayName())
        .setView(view)
        .setNegativeButton(android.R.string.cancel, null)
        .setOnCancelListener(this::sendResult)
        .setPositiveButton(android.R.string.ok, this::sendResult)
        .setNeutralButton(R.string.delete, this::delete)
        .create();
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    sendResult(dialog);
  }

  private Location toLocation() {
    Location result = getOriginal();
    result.setArrival(arrivalView.isChecked());
    result.setDeparture(departureView.isChecked());
    result.setRadius((int) radiusValue.getTag());
    return result;
  }

  private Location getOriginal() {
    return getArguments().getParcelable(EXTRA_ORIGINAL);
  }

  private void sendResult(DialogInterface d, int... i) {
    sendResult(toLocation());
  }

  private void delete(DialogInterface d, int i) {
    sendResult(null);
  }

  private void sendResult(Location result) {
    Intent data = new Intent();
    data.putExtra(EXTRA_ORIGINAL, (Parcelable) getOriginal());
    data.putExtra(EXTRA_LOCATION, (Parcelable) result);
    getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, data);
    dismiss();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelable(EXTRA_LOCATION, toLocation());
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

  @OnClick(R.id.location_url)
  void openUrl() {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(getOriginal().getUrl()));
    startActivity(intent);
  }

  @OnClick(R.id.location_call)
  void openDialer() {
    Intent intent = new Intent(Intent.ACTION_DIAL);
    intent.setData(Uri.parse("tel:" + getOriginal().getPhone()));
    startActivity(intent);
  }

  @OnClick(R.id.location_radius)
  void selectRadius() {
    SeekBarDialog seekBarDialog =
        newSeekBarDialog(R.layout.dialog_radius_seekbar, 75, 1000, toLocation().getRadius());
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
