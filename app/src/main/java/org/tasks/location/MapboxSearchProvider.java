package org.tasks.location;

import static org.tasks.data.Place.newPlace;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
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
      @Nullable MapPosition bias,
      Callback<List<PlaceSearchResult>> onSuccess,
      Callback<String> onError) {
    if (builder == null) {
      String token = context.getString(R.string.mapbox_key);
      Mapbox.getInstance(context, token);
      builder = MapboxGeocoding.builder().autocomplete(true).accessToken(token);
      if (bias != null) {
        builder.proximity(Point.fromLngLat(bias.getLongitude(), bias.getLatitude()));
      }
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
    onSuccess.call(placeSearchResult.getPlace());
  }

  private PlaceSearchResult toSearchResult(CarmenFeature feature) {
    Place place = newPlace(feature);
    return new PlaceSearchResult(feature.id(), place.getName(), place.getDisplayAddress(), place);
  }
}
