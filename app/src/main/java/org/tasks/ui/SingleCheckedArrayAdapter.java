package org.tasks.ui;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;
import static com.todoroo.andlib.utility.AndroidUtilities.preLollipop;
import static org.tasks.preferences.ResourceResolver.getData;
import static org.tasks.preferences.ResourceResolver.getDimen;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import java.util.List;
import org.tasks.R;
import org.tasks.themes.ThemeAccent;

public class SingleCheckedArrayAdapter extends ArrayAdapter<String> {

  @NonNull private final Context context;
  private final ThemeAccent accent;
  private final int alpha;
  private final int tint;

  public SingleCheckedArrayAdapter(
      @NonNull Context context, @NonNull List<String> items, ThemeAccent accent) {
    super(context, R.layout.simple_list_item_single_choice_themed, items);
    this.context = context;
    this.accent = accent;
    this.alpha = (int) (255 * getDimen(context, R.dimen.alpha_secondary));
    this.tint = getData(context, R.attr.icon_tint);
  }

  @NonNull
  @Override
  public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
    if (preLollipop()) {
      ColorStateList tintList =
          new ColorStateList(
              new int[][] {
                new int[] {-android.R.attr.state_checked}, new int[] {android.R.attr.state_checked}
              },
              new int[] {
                ResourcesCompat.getColor(context.getResources(), android.R.color.transparent, null),
                accent.getAccentColor()
              });
      Drawable original = ContextCompat.getDrawable(context, R.drawable.ic_check_black_24dp);
      Drawable wrapped = DrawableCompat.wrap(original.mutate());
      DrawableCompat.setTintList(wrapped, tintList);
      view.setCheckMarkDrawable(wrapped);
    }
    int drawable = getDrawable(position);
    if (drawable > 0) {
      Drawable original = ContextCompat.getDrawable(context, drawable);
      Drawable wrapped = DrawableCompat.wrap(original.mutate());
      int color = getDrawableColor(position);
      if (color == 0) {
        color = tint;
        wrapped.setAlpha(alpha);
      } else if (color == -1) {
        wrapped.setAlpha(0);
      }
      DrawableCompat.setTint(wrapped, color);
      if (atLeastJellybeanMR1()) {
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(wrapped, null, null, null);
      } else {
        view.setCompoundDrawablesWithIntrinsicBounds(wrapped, null, null, null);
      }
    }
    return view;
  }

  protected int getDrawable(int position) {
    return 0;
  }

  protected int getDrawableColor(int position) {
    return 0;
  }
}
