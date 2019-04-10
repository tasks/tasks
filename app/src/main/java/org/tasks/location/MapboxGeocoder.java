package org.tasks.location;

import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;
import static org.tasks.data.Place.newPlace;

import android.content.Context;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import java.io.IOException;
import java.util.List;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.data.Place;
import retrofit2.Response;
import timber.log.Timber;

public class MapboxGeocoder implements Geocoder {

  private final String token;

  public MapboxGeocoder(Context context) {
    token = context.getString(R.string.mapbox_key);
    Mapbox.getInstance(context, token);
  }

  private static String prettyPrint(String json) {
    if (BuildConfig.DEBUG) {
      return new GsonBuilder().setPrettyPrinting().create().toJson(new JsonParser().parse(json));
    }
    return json;
  }

  @Override
  public Place reverseGeocode(MapPosition mapPosition) throws IOException {
    assertNotMainThread();

    Response<GeocodingResponse> response =
        MapboxGeocoding.builder()
            .accessToken(token)
            .query(Point.fromLngLat(mapPosition.getLongitude(), mapPosition.getLatitude()))
            .build()
            .executeCall();
    GeocodingResponse body = response.body();
    if (response.isSuccessful() && body != null) {
      Timber.d(prettyPrint(body.toJson()));
      List<CarmenFeature> features = body.features();
      if (features.size() > 0) {
        return newPlace(features.get(0));
      }
    } else {
      Timber.e(response.errorBody().string());
    }
    return newPlace(mapPosition);
  }
}
