package org.tasks.location;

import static com.mapbox.api.geocoding.v5.GeocodingCriteria.TYPE_ADDRESS;
import static org.tasks.data.Place.newPlace;

import android.content.Context;
import android.os.Bundle;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import java.util.ArrayList;
import java.util.List;
import org.tasks.Callback;
import org.tasks.R;
import org.tasks.data.Place;
import retrofit2.Call;
import retrofit2.Response;

public class MapboxSearchProvider implements PlaceSearchProvider {

  private final Context context;
  private MapboxGeocoding.Builder builder;

  public MapboxSearchProvider(Context context) {
    this.context = context;
  }

  @Override
  public void restoreState(Bundle savedInstanceState) {}

  @Override
  public void saveState(Bundle outState) {}

  @Override
  public int getAttributionRes(boolean dark) {
    return R.drawable.mapbox_logo_icon;
  }

  @Override
  public void search(
      String query,
      MapPosition bias,
      Callback<List<PlaceSearchResult>> onSuccess,
      Callback<String> onError) {
    if (builder == null) {
      String token = context.getString(R.string.mapbox_key);
      Mapbox.getInstance(context, token);
      builder =
          MapboxGeocoding.builder()
              .autocomplete(true)
              .accessToken(token)
              .proximity(Point.fromLngLat(bias.getLongitude(), bias.getLatitude()));
    }

    builder
        .query(query)
        .build()
        .enqueueCall(
            new retrofit2.Callback<GeocodingResponse>() {
              @Override
              public void onResponse(
                  Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                List<PlaceSearchResult> results = new ArrayList<>();
                results.clear();
                for (CarmenFeature feature : response.body().features()) {
                  results.add(toSearchResult(feature));
                }
                onSuccess.call(results);
              }

              @Override
              public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                onError.call(t.getMessage());
              }
            });
  }

  @Override
  public void fetch(
      PlaceSearchResult placeSearchResult, Callback<Place> onSuccess, Callback<String> onError) {
    CarmenFeature carmenFeature = (CarmenFeature) placeSearchResult.getTag();
    org.tasks.data.Place place = newPlace();
    place.setName(placeSearchResult.getName());
    place.setAddress(placeSearchResult.getAddress());
    place.setLatitude(carmenFeature.center().latitude());
    place.setLongitude(carmenFeature.center().longitude());
    onSuccess.call(place);
  }

  private PlaceSearchResult toSearchResult(CarmenFeature feature) {
    String name = getName(feature);
    String address = feature.placeName();
    String replace = String.format("%s, ", name);
    if (address != null && address.startsWith(replace)) {
      address = address.replace(replace, "");
    }
    return new PlaceSearchResult(feature.id(), name, address, feature);
  }

  private String getName(CarmenFeature feature) {
    List<String> types = feature.placeType();
    return types != null && types.contains(TYPE_ADDRESS)
        ? String.format("%s %s", feature.address(), feature.text())
        : feature.text();
  }
}
