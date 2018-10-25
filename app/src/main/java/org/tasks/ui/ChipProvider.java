package org.tasks.ui;

import static org.tasks.preferences.ResourceResolver.getDimen;

import android.content.Context;
import android.content.res.ColorStateList;
import com.google.android.material.chip.Chip;
import com.google.common.collect.Ordering;
import com.todoroo.astrid.api.Filter;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.TagData;
import org.tasks.injection.ForActivity;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

public class ChipProvider {

  private final Context context;
  private final ThemeCache themeCache;
  private final int iconAlpha;

  @Inject
  public ChipProvider(@ForActivity Context context, ThemeCache themeCache) {
    this.context = context;
    this.themeCache = themeCache;
    iconAlpha = (int) (255 * getDimen(context, R.dimen.alpha_secondary));
  }

  public Chip getChip(TagData tagData) {
    Chip chip = new Chip(context);
    chip.setCloseIconVisible(true);
    apply(chip, tagData.getName(), tagData.getColor());
    return chip;
  }

  public void apply(Chip chip, Filter filter) {
    apply(chip, filter.listingTitle, filter.tint);
  }

  private void apply(Chip chip, String name, int theme) {
    ThemeColor color = theme >= 0 ? themeCache.getThemeColor(theme) : themeCache.getUntaggedColor();
    chip.setText(name);
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
