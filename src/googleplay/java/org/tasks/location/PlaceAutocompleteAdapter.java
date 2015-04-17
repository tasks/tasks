package org.tasks.location;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlaceAutocompleteAdapter
        extends ArrayAdapter<PlaceAutocompleteAdapter.PlaceAutocomplete> {

    private static final Logger log = LoggerFactory.getLogger(PlaceAutocompleteAdapter.class);
    private final ManagedGoogleApi managedGoogleApi;

    private List<PlaceAutocomplete> mResultList = new ArrayList<>();

    public PlaceAutocompleteAdapter(ManagedGoogleApi managedGoogleApi, Context context, int resource) {
        super(context, resource);
        this.managedGoogleApi = managedGoogleApi;
    }

    @Override
    public int getCount() {
        return mResultList == null ? 0 : mResultList.size();
    }

    @Override
    public PlaceAutocomplete getItem(int position) {
        return mResultList.get(position);
    }

    public void getAutocomplete(CharSequence constraint) {
        managedGoogleApi.getAutocompletePredictions(constraint.toString(), onResults);
    }

    private ResultCallback<AutocompletePredictionBuffer> onResults = new ResultCallback<AutocompletePredictionBuffer>() {
        @Override
        public void onResult(AutocompletePredictionBuffer autocompletePredictions) {
            final Status status = autocompletePredictions.getStatus();
            if (!status.isSuccess()) {
                Toast.makeText(getContext(), "Error contacting API: " + status.toString(),
                        Toast.LENGTH_SHORT).show();
                log.error("Error getting autocomplete prediction API call: " + status.toString());
                autocompletePredictions.release();
                return;
            }

            log.info("Query completed. Received " + autocompletePredictions.getCount()
                    + " predictions.");

            Iterator<AutocompletePrediction> iterator = autocompletePredictions.iterator();
            List<PlaceAutocomplete> resultList = new ArrayList<>(autocompletePredictions.getCount());
            while (iterator.hasNext()) {
                AutocompletePrediction prediction = iterator.next();
                resultList.add(new PlaceAutocomplete(prediction.getPlaceId(),
                        prediction.getDescription()));
            }

            setResults(resultList);
            autocompletePredictions.release();
        }
    };

    private void setResults(List<PlaceAutocomplete> results) {
        mResultList = results;
        if (mResultList != null && mResultList.size() > 0) {
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
    }

    public class PlaceAutocomplete {

        public CharSequence placeId;
        public CharSequence description;

        PlaceAutocomplete(CharSequence placeId, CharSequence description) {
            this.placeId = placeId;
            this.description = description;
        }

        @Override
        public String toString() {
            return description.toString();
        }
    }
}
