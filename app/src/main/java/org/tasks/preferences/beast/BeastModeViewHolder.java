package org.tasks.preferences.beast;

import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTouch;
import org.tasks.R;

class BeastModeViewHolder extends RecyclerView.ViewHolder {

  private final ItemTouchHelper itemTouchHelper;

  @BindView(R.id.text)
  TextView textView;

  BeastModeViewHolder(View itemView, ItemTouchHelper itemTouchHelper) {
    super(itemView);
    this.itemTouchHelper = itemTouchHelper;
    ButterKnife.bind(this, itemView);
  }

  void setText(String text) {
    textView.setText(text);
  }

  @OnTouch(R.id.grabber)
  boolean onTouch(View view, MotionEvent event) {
    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
      itemTouchHelper.startDrag(this);
    }
    return false;
  }
}
