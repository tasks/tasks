package org.tasks.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.maps.model.LatLng;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.location.Geofence;
import org.tasks.location.GoogleApi;
import org.tasks.location.OnLocationPickedHandler;
import org.tasks.location.PlaceAutocompleteAdapter;

import javax.inject.Inject;

public class LocationPickerDialog extends InjectingDialogFragment implements GoogleApiClient.OnConnectionFailedListener {

    private static final Logger log = LoggerFactory.getLogger(LocationPickerDialog.class);
    private static final int RC_RESOLVE_GPS_ISSUE = 10009;

    private PlaceAutocompleteAdapter mAdapter;

    @Inject FragmentActivity fragmentActivity;
    @Inject GoogleApi googleApi;
    private OnLocationPickedHandler onLocationPickedHandler;
    private DialogInterface.OnCancelListener onCancelListener;

    public void setOnLocationPickedHandler(OnLocationPickedHandler onLocationPickedHandler) {
        this.onLocationPickedHandler = onLocationPickedHandler;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        googleApi.connect(this);

        Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.Tasks_Dialog);
        LayoutInflater themedInflater = inflater.cloneInContext(contextThemeWrapper);
        View layout = themedInflater.inflate(R.layout.location_picker_dialog, null);
        AutoCompleteTextView addressEntry = (AutoCompleteTextView) layout.findViewById(R.id.address_entry);
        addressEntry.setOnItemClickListener(mAutocompleteClickListener);
        mAdapter = new PlaceAutocompleteAdapter(googleApi, fragmentActivity, android.R.layout.simple_list_item_1);
        addressEntry.setAdapter(mAdapter);

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        googleApi.disconnect();
    }

    private void error(String text) {
        log.error(text);
        Toast.makeText(fragmentActivity, text, Toast.LENGTH_LONG).show();
    }

    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceAutocompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            log.info("Autocomplete item selected: " + item.description);
            googleApi.getPlaceDetails(placeId, mUpdatePlaceDetailsCallback);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (places.getStatus().isSuccess()) {
                final Place place = places.get(0);
                LatLng latLng = place.getLatLng();
                Geofence geofence = new Geofence(place.getName().toString(), latLng.latitude, latLng.longitude, 150);
                log.info("Picked {}", geofence);
                onLocationPickedHandler.onLocationPicked(geofence);
                dismiss();
            } else {
                error("Error looking up location details - " + places.getStatus().toString());
            }
            places.release();
        }
    };

    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        this.onCancelListener = onCancelListener;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        if (onCancelListener != null) {
            onCancelListener.onCancel(dialog);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(fragmentActivity, RC_RESOLVE_GPS_ISSUE);
            } catch (IntentSender.SendIntentException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            GooglePlayServicesUtil
                    .getErrorDialog(connectionResult.getErrorCode(), fragmentActivity, RC_RESOLVE_GPS_ISSUE)
                    .show();
        }
    }
}
