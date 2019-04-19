package org.tasks.preferences;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.google.gson.GsonBuilder;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.tasks.R;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.ui.MenuColorizer;
import timber.log.Timber;

public class AttributionActivity extends ThemedInjectingAppCompatActivity {

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.list)
  RecyclerView recyclerView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_attributions);

    ButterKnife.bind(this);

    toolbar.setTitle(R.string.third_party_licenses);
    toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24px);
    toolbar.setNavigationOnClickListener(v -> finish());
    MenuColorizer.colorToolbar(this, toolbar);

    recyclerView.setLayoutManager(new LinearLayoutManager(this));
  }

  @Override
  protected void onResume() {
    super.onResume();

    ViewModelProviders.of(this).get(ViewModel.class).observe(this, this::updateAttributions);
  }

  private void updateAttributions(List<LibraryAttribution> libraryAttributions) {
    SectionedRecyclerViewAdapter adapter = new SectionedRecyclerViewAdapter();
    ImmutableListMultimap<String, LibraryAttribution> index =
        Multimaps.index(libraryAttributions, LibraryAttribution::getLicense);
    ArrayList<String> licenses = new ArrayList<>(index.keySet());
    Collections.sort(licenses);
    for (String license : licenses) {
      adapter.addSection(new AttributionSection(license, index.get(license)));
    }
    recyclerView.setAdapter(adapter);
    Timber.d(libraryAttributions.toString());
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  public static class AttributionList {
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

  public static class ViewModel extends androidx.lifecycle.ViewModel {
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
                        new InputStreamReader(licenses, Charset.forName("UTF-8"));
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
  }
}
