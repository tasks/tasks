package org.tasks.tags;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import kotlin.jvm.functions.Function2;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.data.TagData;
import org.tasks.tags.CheckBoxTriStates.State;
import org.tasks.themes.ColorProvider;
import org.tasks.themes.CustomIcons;
import org.tasks.themes.ThemeColor;

class TagRecyclerAdapter extends RecyclerView.Adapter<TagPickerViewHolder> {

  private final AsyncListDiffer<TagData> differ;
  private final Context context;
  private final TagPickerViewModel viewModel;
  private final Inventory inventory;
  private final ColorProvider colorProvider;
  private final Function2<TagData, Boolean, State> callback;

  TagRecyclerAdapter(
      Context context,
      TagPickerViewModel viewModel,
      Inventory inventory,
      ColorProvider colorProvider,
      Function2<TagData, Boolean, State> callback) {
    this.context = context;
    this.viewModel = viewModel;
    this.inventory = inventory;
    this.colorProvider = colorProvider;
    this.callback = callback;
    differ = new AsyncListDiffer<>(this, new TagDiffCallback());
  }

  @NonNull
  @Override
  public TagPickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(context).inflate(R.layout.row_tag_picker, parent, false);
    return new TagPickerViewHolder(context, view, callback);
  }

  @Override
  public void onBindViewHolder(@NonNull TagPickerViewHolder holder, int position) {
    TagData tagData = differ.getCurrentList().get(position);
    holder.bind(tagData, getColor(tagData), getIcon(tagData), viewModel.getState(tagData));
  }

  @Override
  public int getItemCount() {
    return differ.getCurrentList().size();
  }

  private int getColor(TagData tagData) {
    if (tagData.getColor() != 0) {
      ThemeColor themeColor = colorProvider.getThemeColor(tagData.getColor(), true);
      if (inventory.purchasedThemes() || themeColor.isFree()) {
        return themeColor.getPrimaryColor();
      }
    }
    return context.getColor(R.color.icon_tint_with_alpha);
  }

  private @Nullable Integer getIcon(TagData tagData) {
    return tagData.getIcon() < 1000 || inventory.hasPro()
        ? CustomIcons.getIconResId(tagData.getIcon())
        : null;
  }

  public void submitList(List<TagData> tagData) {
    differ.submitList(tagData);
  }
}
