package org.tasks.location;

import android.content.Context;
import android.os.Bundle;
import java.util.List;
import org.tasks.Callback;
import org.tasks.data.Place;

public class GooglePlacesSearchProvider implements PlaceSearchProvider {

  public GooglePlacesSearchProvider(Context context) {}

  @Override
  public void restoreState(Bundle savedInstanceState) {}

  @Override
  public void saveState(Bundle outState) {}

  @Override
  public void search(
      String query,
      MapPosition bias,
      Callback<List<PlaceSearchResult>> onSuccess,
      Callback<String> onError) {}

  @Override
  public void fetch(
      PlaceSearchResult placeSearchResult, Callback<Place> onSuccess, Callback<String> onError) {}
}
