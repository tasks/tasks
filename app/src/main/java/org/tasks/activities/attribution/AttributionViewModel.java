package org.tasks.activities.attribution;

import android.content.Context;
import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.google.gson.GsonBuilder;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import timber.log.Timber;

@SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
public class AttributionViewModel extends androidx.lifecycle.ViewModel {
  private final MutableLiveData<List<LibraryAttribution>> attributions = new MutableLiveData<>();
  private final CompositeDisposable disposables = new CompositeDisposable();
  private boolean loaded;

  void observe(AppCompatActivity activity, Observer<List<LibraryAttribution>> observer) {
    attributions.observe(activity, observer);
    load(activity);
  }

  private void load(Context context) {
    if (loaded) {
      return;
    }
    loaded = true;
    disposables.add(
        Single.fromCallable(
                () -> {
                  InputStream licenses = context.getAssets().open("licenses.json");
                  InputStreamReader reader =
                      new InputStreamReader(licenses, StandardCharsets.UTF_8);
                  AttributionList list =
                      new GsonBuilder().create().fromJson(reader, AttributionList.class);
                  return list.libraries;
                })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(attributions::setValue, Timber::e));
  }

  @Override
  protected void onCleared() {
    disposables.dispose();
  }

  static class AttributionList {
    List<LibraryAttribution> libraries;
  }

  static class LibraryAttribution {
    String copyrightHolder;
    String license;
    String libraryName;

    String getCopyrightHolder() {
      return copyrightHolder;
    }

    @Keep
    String getLicense() {
      return license;
    }

    String getLibraryName() {
      return libraryName;
    }
  }
}
