package org.tasks.location;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.google.common.base.Strings;
import java.util.List;
import org.tasks.R;

public class LocationSearchAdapter extends RecyclerView.Adapter<ViewHolder>
    implements ListUpdateCallback {

  private final int attributionRes;
  private final OnPredictionPicked callback;
  private final AsyncListDiffer<PlaceSearchResult> differ;

  LocationSearchAdapter(@DrawableRes int attributionRes, OnPredictionPicked callback) {
    this.attributionRes = attributionRes;
    this.callback = callback;
    differ =
        new AsyncListDiffer<>(this, new AsyncDifferConfig.Builder<>(new DiffCallback()).build());
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return viewType == 0
        ? new SearchViewHolder(
            LayoutInflater.from(parent.getContext()).inflate(R.layout.row_place, parent, false),
            callback)
        : new FooterViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_place_footer, parent, false),
            attributionRes);
  }

  void submitList(@Nullable List<PlaceSearchResult> list) {
    differ.submitList(list);
  }

  @Override
  public int getItemCount() {
    return differ.getCurrentList().size() + 1;
  }

  @Override
  public int getItemViewType(int position) {
    return position < getItemCount() - 1 ? 0 : 1;
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    if (getItemViewType(position) == 0) {
      ((SearchViewHolder) holder).bind(differ.getCurrentList().get(position));
    } else {
      ((FooterViewHolder) holder).bind(position);
    }
  }

  @Override
  public void onInserted(int position, int count) {
    notifyItemRangeInserted(position, count);
    updateFooter();
  }

  @Override
  public void onRemoved(int position, int count) {
    notifyItemRangeRemoved(position, count);
    updateFooter();
  }

  @Override
  public void onMoved(int fromPosition, int toPosition) {
    notifyItemMoved(fromPosition, toPosition);
  }

  @Override
  public void onChanged(int position, int count, Object payload) {
    notifyItemRangeChanged(position, count, payload);
  }

  private void updateFooter() {
    notifyItemChanged(getItemCount() - 1);
  }

  public interface OnPredictionPicked {
    void picked(PlaceSearchResult prediction);
  }

  public static class SearchViewHolder extends RecyclerView.ViewHolder {
    private final TextView name;
    private final TextView address;
    private PlaceSearchResult prediction;

    SearchViewHolder(@NonNull View itemView, OnPredictionPicked onPredictionPicked) {
      super(itemView);
      itemView.setOnClickListener(v -> onPredictionPicked.picked(prediction));
      name = itemView.findViewById(R.id.name);
      address = itemView.findViewById(R.id.address);
      itemView.findViewById(R.id.place_icon).setVisibility(View.INVISIBLE);
    }

    public void bind(PlaceSearchResult prediction) {
      this.prediction = prediction;
      CharSequence name = prediction.getName();
      CharSequence address = prediction.getAddress();
      this.name.setText(name);
      if (address == null
          || Strings.isNullOrEmpty(address.toString())
          || address.toString().equals(name.toString())) {
        this.address.setVisibility(View.GONE);
      } else {
        this.address.setText(address);
        this.address.setVisibility(View.VISIBLE);
      }
    }
  }

  public static class FooterViewHolder extends RecyclerView.ViewHolder {

    View divider;

    FooterViewHolder(@NonNull View itemView, @DrawableRes int attributionRes) {
      super(itemView);

      ((ImageView) itemView.findViewById(R.id.place_attribution)).setImageResource(attributionRes);
      divider = itemView.findViewById(R.id.divider);
    }

    void bind(int position) {
      divider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
    }
  }

  public static class DiffCallback extends ItemCallback<PlaceSearchResult> {

    @Override
    public boolean areItemsTheSame(
        @NonNull PlaceSearchResult oldItem, @NonNull PlaceSearchResult newItem) {
      return oldItem.getId().equals(newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(
        @NonNull PlaceSearchResult oldItem, @NonNull PlaceSearchResult newItem) {
      return oldItem.equals(newItem);
    }
  }
}
