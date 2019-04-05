package org.tasks.location;

import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import java.util.List;
import org.tasks.Callback;
import org.tasks.data.Place;

public interface PlaceSearchProvider {
  void restoreState(Bundle savedInstanceState);

  void saveState(Bundle outState);

  @DrawableRes int getAttributionRes(boolean dark);

  void search(
      String query,
      @Nullable MapPosition bias,
      Callback<List<PlaceSearchResult>> onSuccess,
      Callback<String> onError);

  void fetch(
      PlaceSearchResult placeSearchResult, Callback<Place> onSuccess, Callback<String> onError);
}
