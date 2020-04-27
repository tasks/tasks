package org.tasks.activities.attribution;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.tasks.R;
import org.tasks.activities.attribution.AttributionViewModel.LibraryAttribution;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
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
    themeColor.apply(toolbar);

    recyclerView.setLayoutManager(new LinearLayoutManager(this));
  }

  @Override
  protected void onResume() {
    super.onResume();

    new ViewModelProvider(this)
        .get(AttributionViewModel.class)
        .observe(this, this::updateAttributions);
  }

  private void updateAttributions(List<LibraryAttribution> libraryAttributions) {
    List<AttributionRow> rows = new ArrayList<>();
    ImmutableListMultimap<String, LibraryAttribution> index =
        Multimaps.index(libraryAttributions, LibraryAttribution::getLicense);
    ArrayList<String> licenses = new ArrayList<>(index.keySet());
    Collections.sort(licenses);
    for (String license : licenses) {
      rows.add(new AttributionRow(license));
      ImmutableList<LibraryAttribution> libraries = index.get(license);
      ImmutableListMultimap<String, LibraryAttribution> idx =
          Multimaps.index(libraries, LibraryAttribution::getCopyrightHolder);
      List<String> copyrightHolders = newArrayList(idx.keySet());
      Collections.sort(copyrightHolders);
      for (String copyrightHolder : copyrightHolders) {
        List<String> libs = newArrayList(transform(idx.get(copyrightHolder),
            a -> "\u2022 " + a.getLibraryName()));
        Collections.sort(libs);
        rows.add(new AttributionRow(copyrightHolder, Joiner.on("\n").join(libs)));
      }
    }
    recyclerView.setAdapter(new AttributionAdapter(rows));
    Timber.d(libraryAttributions.toString());
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
