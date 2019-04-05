package org.tasks.location;

import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import com.google.common.base.Strings;
import java.util.Collections;
import java.util.List;
import org.tasks.Event;
import org.tasks.data.Place;

public class PlaceSearchViewModel extends ViewModel {
  private PlaceSearchProvider searchProvider;

  private MutableLiveData<List<PlaceSearchResult>> searchResults = new MutableLiveData<>();
  private MutableLiveData<Event<String>> error = new MutableLiveData<>();
  private MutableLiveData<Place> selection = new MutableLiveData<>();

  void setSearchProvider(PlaceSearchProvider searchProvider) {
    this.searchProvider = searchProvider;
  }

  void observe(
      LifecycleOwner owner,
      Observer<List<PlaceSearchResult>> onResults,
      Observer<Place> onSelection,
      Observer<Event<String>> onError) {
    searchResults.observe(owner, onResults);
    selection.observe(owner, onSelection);
    error.observe(owner, onError);
  }

  void saveState(Bundle outState) {
    searchProvider.saveState(outState);
  }

  void restoreState(Bundle savedInstanceState) {
    searchProvider.restoreState(savedInstanceState);
  }

  public void query(String query, @Nullable MapPosition bias) {
    assertMainThread();

    if (Strings.isNullOrEmpty(query)) {
      searchResults.postValue(Collections.emptyList());
    } else {
      searchProvider.search(query, bias, searchResults::setValue, this::setError);
    }
  }

  public void fetch(PlaceSearchResult result) {
    searchProvider.fetch(result, selection::setValue, this::setError);
  }

  private void setError(String message) {
    error.setValue(new Event<>(message));
  }
}
