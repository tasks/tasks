package org.tasks.preferences.beast;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.tasks.R;

public class BeastModeRecyclerAdapter extends RecyclerView.Adapter<BeastModeViewHolder> {

  private final List<String> items = new ArrayList<>();
  private final HashMap<String, String> prefsToDescriptions = new HashMap<>();
  private final ItemTouchHelper itemTouchHelper;

  public BeastModeRecyclerAdapter(Context context, List<String> items) {
    this.items.addAll(items);
    itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback());
    buildDescriptionMap(context.getResources());
  }

  public void applyToRecyclerView(RecyclerView recyclerView) {
    recyclerView.setAdapter(this);
    itemTouchHelper.attachToRecyclerView(recyclerView);
  }

  public List<String> getItems() {
    return items;
  }

  public void setItems(List<String> items) {
    this.items.clear();
    this.items.addAll(items);
    notifyDataSetChanged();
  }

  @Override
  public BeastModeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.preference_draggable_row, parent, false);
    return new BeastModeViewHolder(view, itemTouchHelper);
  }

  @Override
  public void onBindViewHolder(final BeastModeViewHolder holder, int position) {
    String key = items.get(position);
    String value = prefsToDescriptions.get(key);
    holder.setText(value);
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  private void buildDescriptionMap(Resources r) {
    String[] keys = r.getStringArray(R.array.TEA_control_sets_prefs);
    String[] descriptions = r.getStringArray(R.array.TEA_control_sets_beast);
    for (int i = 0; i < keys.length && i < descriptions.length; i++) {
      prefsToDescriptions.put(keys[i], descriptions[i]);
    }
  }

  private class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

    @Override
    public boolean isItemViewSwipeEnabled() {
      return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
      return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
    }

    @Override
    public boolean onMove(
        RecyclerView recyclerView, RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
      int fromPosition = from.getAdapterPosition();
      int toPosition = to.getAdapterPosition();
      Collections.swap(items, fromPosition, toPosition);
      notifyItemMoved(fromPosition, toPosition);
      return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {}
  }
}
