package org.tasks.preferences.beast;

import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.tasks.databinding.PreferenceDraggableRowBinding;

class BeastModeViewHolder extends RecyclerView.ViewHolder {

  private final ItemTouchHelper itemTouchHelper;
  private final TextView textView;

  BeastModeViewHolder(PreferenceDraggableRowBinding binding, ItemTouchHelper itemTouchHelper) {
    super(binding.getRoot());
    this.itemTouchHelper = itemTouchHelper;
    textView = binding.text;
    binding.grabber.setOnTouchListener(this::onTouch);
  }

  void setText(String text) {
    textView.setText(text);
  }

  private boolean onTouch(View v, MotionEvent event) {
    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
      itemTouchHelper.startDrag(this);
    }
    return false;
  }
}
