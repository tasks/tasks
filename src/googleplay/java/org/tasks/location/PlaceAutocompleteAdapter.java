package org.tasks.location;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;

public class PlaceAutocompleteAdapter
        extends ArrayAdapter<PlaceAutocompleteAdapter.PlaceAutocomplete> {

    private final GoogleApi googleApi;

    private List<PlaceAutocomplete> mResultList = new ArrayList<>();

    public PlaceAutocompleteAdapter(GoogleApi googleApi, Context context, int resource) {
        super(context, resource);
        this.googleApi = googleApi;
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
        googleApi.getAutocompletePredictions(constraint.toString(), onResults);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    getAutocomplete(constraint);
                }
                filterResults.values = mResultList;
                filterResults.count = mResultList.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

            }
        };
    }

    private ResultCallback<AutocompletePredictionBuffer> onResults = new ResultCallback<AutocompletePredictionBuffer>() {
        @Override
        public void onResult(AutocompletePredictionBuffer autocompletePredictions) {
            final Status status = autocompletePredictions.getStatus();
            if (!status.isSuccess()) {
                Toast.makeText(getContext(), "Error contacting API: " + status.toString(),
                        Toast.LENGTH_SHORT).show();
                Timber.e("Error getting autocomplete prediction API call: %s", status.toString());
                autocompletePredictions.release();
                return;
            }

            Timber.i("Query completed. Received %s predictions", autocompletePredictions.getCount());

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
