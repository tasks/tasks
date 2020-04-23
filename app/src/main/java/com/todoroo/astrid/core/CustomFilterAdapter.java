package com.todoroo.astrid.core;

import static com.google.common.collect.Lists.transform;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.tasks.Callback;
import org.tasks.R;
import org.tasks.locale.Locale;

public class CustomFilterAdapter extends RecyclerView.Adapter<CriterionViewHolder> {

  private final Callback<String> onClick;
  private final Locale locale;
  private final AsyncListDiffer<CriterionInstance> differ;

  public CustomFilterAdapter(
      List<CriterionInstance> objects, Locale locale, Callback<String> onClick) {
    this.locale = locale;
    this.onClick = onClick;
    differ = new AsyncListDiffer<>(this, new CriterionDiffCallback());
    submitList(objects);
  }

  @NonNull
  @Override
  public CriterionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_filter_row, parent, false);
    return new CriterionViewHolder(view, locale, onClick);
  }

  @Override
  public void onBindViewHolder(@NonNull CriterionViewHolder holder, int position) {
    holder.bind(getItems().get(position));
  }

  @Override
  public int getItemCount() {
    return getItems().size();
  }

  public void submitList(List<CriterionInstance> criteria) {
    differ.submitList(ImmutableList.copyOf(transform(criteria, CriterionInstance::new)));
  }

  private List<CriterionInstance> getItems() {
    return differ.getCurrentList();
  }
}
