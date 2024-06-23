package org.tasks.location;

import static org.tasks.Strings.isNullOrEmpty;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.typeface.library.googlematerial.OutlinedGoogleMaterial;

import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.data.PlaceExtensionsKt;
import org.tasks.data.PlaceUsage;
import org.tasks.data.entity.Place;
import org.tasks.filters.FilterExtensionsKt;
import org.tasks.filters.PlaceFilter;
import org.tasks.location.LocationPickerAdapter.PlaceViewHolder;
import org.tasks.themes.ColorProvider;
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
            context,
        LayoutInflater.from(parent.getContext()).inflate(R.layout.row_place, parent, false),
        callback);
  }

  @Override
  public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
    PlaceUsage place = getItem(position);
    PlaceFilter filter = new PlaceFilter(place.getPlace(), 0);
    String icon = FilterExtensionsKt.getIcon(filter, inventory);
    holder.bind(place, getColor(place.getColor()), icon);
  }

  private int getColor(int tint) {
    if (tint != 0) {
      ThemeColor color = colorProvider.getThemeColor(tint, true);
      if (color.isFree() || inventory.purchasedThemes()) {
        return color.getPrimaryColor();
      }
    }
    return context.getColor(R.color.text_primary);
  }

  public interface OnLocationPicked {
    void picked(Place place);

    void settings(Place place);
  }

  public static class PlaceViewHolder extends RecyclerView.ViewHolder {
    private final Context context;
    private final TextView name;
    private final TextView address;
    private final ImageView icon;
    private Place place;

    PlaceViewHolder(Context context, @NonNull View itemView, OnLocationPicked onLocationPicked) {
      super(itemView);
      this.context = context;
      itemView.setOnClickListener(v -> onLocationPicked.picked(place));
      name = itemView.findViewById(R.id.name);
      address = itemView.findViewById(R.id.address);
      icon = itemView.findViewById(R.id.place_icon);
      itemView
          .findViewById(R.id.location_settings)
          .setOnClickListener(v -> onLocationPicked.settings(place));
    }

    void bind(PlaceUsage placeUsage, int color, String icon) {
      place = placeUsage.place;
      String name = PlaceExtensionsKt.getDisplayName(place);
      String address = place.getDisplayAddress();
      Drawable drawable = new IconicsDrawable(
              context,
              OutlinedGoogleMaterial.INSTANCE.getIcon("gmo_" + icon)
      ).mutate();
      this.icon.setImageDrawable(drawable);
      this.icon.getDrawable().setTint(color);
      this.name.setText(name);
      if (isNullOrEmpty(address) || address.equals(name)) {
        this.address.setVisibility(View.GONE);
      } else {
        this.address.setText(address);
        this.address.setVisibility(View.VISIBLE);
      }
    }
  }

  static class DiffCallback extends ItemCallback<PlaceUsage> {

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
