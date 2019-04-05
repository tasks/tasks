package org.tasks.location;

import static java.util.Arrays.asList;
import static org.tasks.data.Place.newPlace;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place.Field;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest.Builder;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.ArrayList;
import java.util.List;
import org.tasks.Callback;
import org.tasks.R;
import org.tasks.data.Place;

public class GooglePlacesSearchProvider implements PlaceSearchProvider {

  private static final String EXTRA_SESSION_TOKEN = "extra_session_token";
  private final Context context;

  private AutocompleteSessionToken token;
  private PlacesClient placesClient;

  public GooglePlacesSearchProvider(Context context) {
    this.context = context;
  }

  @Override
  public void restoreState(Bundle savedInstanceState) {
    token = savedInstanceState.getParcelable(EXTRA_SESSION_TOKEN);
  }

  @Override
  public void saveState(Bundle outState) {}

  @Override
  public int getAttributionRes(boolean dark) {
    return dark
        ? R.drawable.places_powered_by_google_dark
        : R.drawable.places_powered_by_google_light;
  }

  @Override
  public void search(
      String query,
      @Nullable MapPosition bias,
      Callback<List<PlaceSearchResult>> onSuccess,
      Callback<String> onError) {
    if (!Places.isInitialized()) {
      Places.initialize(context, context.getString(R.string.google_key));
    }
    if (placesClient == null) {
      placesClient = Places.createClient(context);
    }
    if (token == null) {
      token = AutocompleteSessionToken.newInstance();
    }
    Builder request =
        FindAutocompletePredictionsRequest.builder().setSessionToken(token).setQuery(query);
    if (bias != null) {
      request.setLocationBias(
          RectangularBounds.newInstance(
              LatLngBounds.builder()
                  .include(new LatLng(bias.getLatitude(), bias.getLongitude()))
                  .build()));
    }
    placesClient
        .findAutocompletePredictions(request.build())
        .addOnSuccessListener(
            response -> onSuccess.call(toSearchResults(response.getAutocompletePredictions())))
        .addOnFailureListener(e -> onError.call(e.getMessage()));
  }

  @Override
  public void fetch(
      PlaceSearchResult placeSearchResult, Callback<Place> onSuccess, Callback<String> onError) {
    placesClient
        .fetchPlace(
            FetchPlaceRequest.builder(
                    placeSearchResult.getId(),
                    asList(
                        Field.ID,
                        Field.LAT_LNG,
                        Field.ADDRESS,
                        Field.WEBSITE_URI,
                        Field.NAME,
                        Field.PHONE_NUMBER))
                .setSessionToken(token)
                .build())
        .addOnSuccessListener(result -> onSuccess.call(toPlace(result)))
        .addOnFailureListener(e -> onError.call(e.getMessage()));
  }

  private List<PlaceSearchResult> toSearchResults(List<AutocompletePrediction> predictions) {
    List<PlaceSearchResult> results = new ArrayList<>();
    for (AutocompletePrediction prediction : predictions) {
      results.add(
          new PlaceSearchResult(
              prediction.getPlaceId(),
              prediction.getPrimaryText(null).toString(),
              prediction.getSecondaryText(null).toString()));
    }
    return results;
  }

  private Place toPlace(FetchPlaceResponse fetchPlaceResponse) {
    com.google.android.libraries.places.api.model.Place place = fetchPlaceResponse.getPlace();
    Place result = newPlace();
    result.setName(place.getName());
    CharSequence address = place.getAddress();
    if (address != null) {
      result.setAddress(place.getAddress());
    }
    CharSequence phoneNumber = place.getPhoneNumber();
    if (phoneNumber != null) {
      result.setPhone(phoneNumber.toString());
    }
    Uri uri = place.getWebsiteUri();
    if (uri != null) {
      result.setUrl(uri.toString());
    }
    LatLng latLng = place.getLatLng();
    result.setLatitude(latLng.latitude);
    result.setLongitude(latLng.longitude);
    return result;
  }
}
