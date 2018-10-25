package org.tasks.ui;

import static org.tasks.preferences.ResourceResolver.getDimen;

import android.content.Context;
import android.content.res.ColorStateList;
import com.google.android.material.chip.Chip;
import com.todoroo.astrid.api.Filter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ForActivity;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

public class ChipProvider {

  private final ThemeCache themeCache;
  private final int iconAlpha;

  @Inject
  public ChipProvider(@ForActivity Context context, ThemeCache themeCache) {
    this.themeCache = themeCache;
    iconAlpha = (int) (255 * getDimen(context, R.dimen.alpha_secondary));
  }

  public void apply(Chip chip, Filter filter) {
    int tint = filter.tint;
    ThemeColor color = tint >= 0 ? themeCache.getThemeColor(tint) : themeCache.getUntaggedColor();
    chip.setText(filter.listingTitle);
    chip.setCloseIconTint(
        new ColorStateList(new int[][] {new int[] {}}, new int[] {color.getActionBarTint()}));
    chip.setTextColor(color.getActionBarTint());
    chip.getChipDrawable().setAlpha(iconAlpha);
    chip.setChipBackgroundColor(
        new ColorStateList(
            new int[][] {
              new int[] {-android.R.attr.state_checked}, new int[] {android.R.attr.state_checked}
            },
            new int[] {color.getPrimaryColor(), color.getPrimaryColor()}));
  }
}
