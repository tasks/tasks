package org.tasks.location;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.common.base.Strings;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.data.Place;
import org.tasks.data.PlaceUsage;
import org.tasks.location.LocationPickerAdapter.PlaceViewHolder;
import org.tasks.themes.ColorProvider;
import org.tasks.themes.CustomIcons;
import org.tasks.themes.DrawableUtil;
import org.tasks.themes.ThemeColor;

public class LocationPickerAdapter extends ListAdapter<PlaceUsage, PlaceViewHolder> {

  private final Context context;
  private final Inventory inventory;
  private final ColorProvider colorProvider;
  private final OnLocationPicked callback;

  LocationPickerAdapter(
      Context context,
      Inventory inventory,
      ColorProvider colorProvider,
      OnLocationPicked callback) {
    super(new DiffCallback());
    this.context = context;
    this.inventory = inventory;
    this.colorProvider = colorProvider;

    this.callback = callback;
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).place.getId();
  }

  @NonNull
  @Override
  public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new PlaceViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(R.layout.row_place, parent, false),
        callback);
  }

  @Override
  public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
    PlaceUsage place = getItem(position);
    holder.bind(place, getColor(place.getColor()), getIcon(place.getIcon()));
  }

  private int getColor(int tint) {
    if (tint != 0) {
      ThemeColor color = colorProvider.getThemeColor(tint, true);
      if (color.isFree() || inventory.purchasedThemes()) {
        return color.getPrimaryColor();
      }
    }
    return ContextCompat.getColor(context, R.color.text_primary);
  }

  private int getIcon(int index) {
    if (index < 1000 || inventory.hasPro()) {
      Integer icon = CustomIcons.getIconResId(index);
      if (icon != null) {
        return icon;
      }
    }
    return R.drawable.ic_outline_place_24px;
  }

  public interface OnLocationPicked {
    void picked(Place place);

    void settings(Place place);
  }

  public static class PlaceViewHolder extends RecyclerView.ViewHolder {
    private final TextView name;
    private final TextView address;
    private final ImageView icon;
    private Place place;

    PlaceViewHolder(@NonNull View itemView, OnLocationPicked onLocationPicked) {
      super(itemView);
      itemView.setOnClickListener(v -> onLocationPicked.picked(place));
      name = itemView.findViewById(R.id.name);
      address = itemView.findViewById(R.id.address);
      icon = itemView.findViewById(R.id.place_icon);
      itemView
          .findViewById(R.id.location_settings)
          .setOnClickListener(v -> onLocationPicked.settings(place));
    }

    public void bind(PlaceUsage placeUsage, int color, int icon) {
      place = placeUsage.place;
      String name = place.getDisplayName();
      String address = place.getDisplayAddress();
      Drawable wrapped = DrawableUtil.getWrapped(itemView.getContext(), icon);
      this.icon.setImageDrawable(wrapped);
      DrawableCompat.setTint(this.icon.getDrawable(), color);
      this.name.setText(name);
      if (Strings.isNullOrEmpty(address) || address.equals(name)) {
        this.address.setVisibility(View.GONE);
      } else {
        this.address.setText(address);
        this.address.setVisibility(View.VISIBLE);
      }

    }
  }

  public static class DiffCallback extends ItemCallback<PlaceUsage> {

    @Override
    public boolean areItemsTheSame(@NonNull PlaceUsage oldItem, @NonNull PlaceUsage newItem) {
      return oldItem.place.getUid().equals(newItem.place.getUid());
    }

    @Override
    public boolean areContentsTheSame(@NonNull PlaceUsage oldItem, @NonNull PlaceUsage newItem) {
      return oldItem.equals(newItem);
    }
  }
}
