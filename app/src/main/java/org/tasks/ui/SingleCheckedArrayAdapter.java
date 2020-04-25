package org.tasks.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import java.util.List;
import org.tasks.R;

public class SingleCheckedArrayAdapter extends ArrayAdapter<String> {

  @NonNull private final Context context;
  private final int alpha;
  private final int tint;

  public SingleCheckedArrayAdapter(@NonNull Context context, @NonNull List<String> items) {
    super(context, R.layout.simple_list_item_single_checkmark, items);
    this.context = context;
    this.alpha =
        (int) (255 * ResourcesCompat.getFloat(context.getResources(), R.dimen.alpha_secondary));
    this.tint = context.getColor(R.color.icon_tint);
  }

  @NonNull
  @Override
  public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
    int drawable = getDrawable();
    if (drawable > 0) {
      Drawable original = context.getDrawable(drawable);
      Drawable wrapped = original.mutate();
      int color = getDrawableColor(position);
      if (color == 0) {
        color = tint;
        wrapped.setAlpha(alpha);
      } else if (color == -1) {
        wrapped.setAlpha(0);
      }
      wrapped.setTint(color);
      view.setCompoundDrawablesRelativeWithIntrinsicBounds(wrapped, null, null, null);
    }
    return view;
  }

  protected int getDrawable() {
    return 0;
  }

  protected int getDrawableColor(int position) {
    return 0;
  }
}
