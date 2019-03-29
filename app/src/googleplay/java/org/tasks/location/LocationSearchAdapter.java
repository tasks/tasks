package org.tasks.location;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.common.base.Strings;
import org.tasks.R;
import org.tasks.location.LocationSearchAdapter.SearchViewHolder;

public class LocationSearchAdapter extends ListAdapter<PlaceSearchResult, SearchViewHolder> {

  private final OnPredictionPicked callback;

  LocationSearchAdapter(OnPredictionPicked callback) {
    super(new DiffCallback());

    this.callback = callback;
  }

  @NonNull
  @Override
  public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new SearchViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(R.layout.row_place, parent, false),
        callback);
  }

  @Override
  public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
    holder.bind(getItem(position));
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
      if (address == null || Strings.isNullOrEmpty(address.toString()) || address.toString().equals(name.toString())) {
        this.address.setVisibility(View.GONE);
      } else {
        this.address.setText(address);
        this.address.setVisibility(View.VISIBLE);
      }
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
