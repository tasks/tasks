package org.tasks.preferences;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.tasks.R;
import org.tasks.preferences.AttributionActivity.LibraryAttribution;

public class AttributionSection extends StatelessSection {

  private final String license;
  private final List<Entry<String, String>> attributions;

  AttributionSection(String license, List<LibraryAttribution> attributions) {
    super(
        SectionParameters.builder()
            .itemResourceId(R.layout.row_attribution)
            .headerResourceId(R.layout.row_attribution_header)
            .build());
    this.license = license;
    ImmutableListMultimap<String, LibraryAttribution> index =
        Multimaps.index(attributions, LibraryAttribution::getCopyrightHolder);
    List<String> copyrightHolders = newArrayList(index.keySet());
    Collections.sort(copyrightHolders);
    Map<String, String> map = newLinkedHashMap();
    for (String copyrightHolder : copyrightHolders) {
      List<String> libraries = newArrayList(transform(index.get(copyrightHolder),
          a -> "\u2022 " + a.getLibraryName()));
      Collections.sort(libraries);
      map.put(copyrightHolder, Joiner.on("\n").join(libraries));
    }
    this.attributions = newArrayList(map.entrySet());
  }

  @Override
  public int getContentItemsTotal() {
    return attributions.size();
  }

  @Override
  public ViewHolder getHeaderViewHolder(View view) {
    return new LicenseHeader(view);
  }

  @Override
  public ViewHolder getItemViewHolder(View view) {
    return new LicenseRow(view);
  }

  @Override
  public void onBindHeaderViewHolder(ViewHolder holder) {
    ((LicenseHeader) holder).bind(license);
  }

  @Override
  public void onBindItemViewHolder(ViewHolder holder, int position) {
    Entry<String, String> entry = attributions.get(position);
    ((LicenseRow) holder).bind(entry.getKey(), entry.getValue());
  }

  static class LicenseHeader extends RecyclerView.ViewHolder {
    @BindView(R.id.license_name)
    TextView licenseName;

    LicenseHeader(@NonNull View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    void bind(String license) {
      licenseName.setText(license);
    }
  }

  static class LicenseRow extends RecyclerView.ViewHolder {

    @BindView(R.id.copyright_holder)
    TextView copyrightHolder;

    @BindView(R.id.libraries)
    TextView libraries;

    LicenseRow(@NonNull View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(String copyrightHolder, String libraries) {
      this.copyrightHolder.setText(copyrightHolder);
      this.libraries.setText(libraries);
    }
  }
}
