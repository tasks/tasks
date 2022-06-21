package com.todoroo.astrid.core;

import static com.google.common.collect.Lists.transform;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.ImmutableList;

import org.tasks.R;

import java.util.List;
import java.util.Locale;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class CustomFilterAdapter extends RecyclerView.Adapter<CriterionViewHolder> implements
    ListUpdateCallback {

  private final Function1<String, Unit> onClick;
  private final Locale locale;
  private final AsyncListDiffer<CriterionInstance> differ;

  public CustomFilterAdapter(
          List<CriterionInstance> objects, Locale locale, Function1<String, Unit> onClick) {
    this.locale = locale;
    this.onClick = onClick;
    differ = new AsyncListDiffer<>(this, new AsyncDifferConfig.Builder<>(new CriterionDiffCallback()).build());
    submitList(objects);
  }

  @NonNull
  @Override
  public CriterionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    Context context = parent.getContext();
    View view = LayoutInflater.from(context).inflate(R.layout.custom_filter_row, parent, false);
    return new CriterionViewHolder(context, view, locale, onClick);
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

  @Override
  public void onInserted(int position, int count) {
    notifyItemRangeInserted(position, count);
  }

  @Override
  public void onRemoved(int position, int count) {
    notifyItemRangeRemoved(position, count);
  }

  @Override
  public void onMoved(int fromPosition, int toPosition) {
    notifyDataSetChanged();
  }

  @Override
  public void onChanged(int position, int count, @Nullable Object payload) {
    notifyItemRangeChanged(position, count);
  }
}
