package org.tasks.ui;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.removeIf;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.newHashSet;
import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static org.tasks.themes.ThemeColor.newThemeColor;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.chip.Chip;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.TagFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.data.TaskContainer;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import org.tasks.themes.CustomIcons;
import org.tasks.themes.ThemeColor;

@ApplicationScope
public class ChipProvider {

  private final Map<String, GtasksFilter> googleTaskLists = new HashMap<>();
  private final Map<String, CaldavFilter> caldavCalendars = new HashMap<>();
  private final Map<String, TagFilter> tagDatas = new HashMap<>();
  private final Inventory inventory;
  private final int iconAlpha;
  private final LocalBroadcastManager localBroadcastManager;
  private final Ordering<TagFilter> orderByName =
      new Ordering<TagFilter>() {
        @Override
        public int compare(TagFilter left, TagFilter right) {
          return left.listingTitle.compareTo(right.listingTitle);
        }
      };
  private boolean filled;
  private boolean showIcon;
  private boolean showText;

  @Inject
  public ChipProvider(
      @ForApplication Context context,
      Inventory inventory,
      GoogleTaskListDao googleTaskListDao,
      CaldavDao caldavDao,
      TagDataDao tagDataDao,
      LocalBroadcastManager localBroadcastManager,
      Preferences preferences) {
    this.inventory = inventory;
    this.localBroadcastManager = localBroadcastManager;
    iconAlpha =
        (int) (255 * ResourcesCompat.getFloat(context.getResources(), R.dimen.alpha_secondary));

    googleTaskListDao.subscribeToLists().observeForever(this::updateGoogleTaskLists);
    caldavDao.subscribeToCalendars().observeForever(this::updateCaldavCalendars);
    tagDataDao.subscribeToTags().observeForever(this::updateTags);
    setStyle(preferences.getIntegerFromString(R.string.p_chip_style, 0));
    setAppearance(preferences.getIntegerFromString(R.string.p_chip_appearance, 0));
  }

  public void setStyle(int style) {
    filled = style == 1;
  }

  public void setAppearance(int appearance) {
    showText = appearance != 2;
    showIcon = appearance != 1;
  }

  private void updateGoogleTaskLists(List<GoogleTaskList> updated) {
    googleTaskLists.clear();
    for (GoogleTaskList update : updated) {
      googleTaskLists.put(update.getRemoteId(), new GtasksFilter(update));
    }
    localBroadcastManager.broadcastRefresh();
  }

  private void updateCaldavCalendars(List<CaldavCalendar> updated) {
    caldavCalendars.clear();
    for (CaldavCalendar update : updated) {
      caldavCalendars.put(update.getUuid(), new CaldavFilter(update));
    }
    localBroadcastManager.broadcastRefresh();
  }

  private void updateTags(List<TagData> updated) {
    tagDatas.clear();
    for (TagData update : updated) {
      tagDatas.put(update.getRemoteId(), new TagFilter(update));
    }
    localBroadcastManager.broadcastRefresh();
  }

  public List<Chip> getChips(
      Activity activity,
      Filter filter,
      boolean isSubtask,
      boolean hideSubtaskChip,
      TaskContainer task) {
    assertMainThread();

    List<Chip> chips = new ArrayList<>();
    if (!hideSubtaskChip && task.hasChildren()) {
      Chip chip = newChip(activity, task);
      apply(
          activity,
          chip,
          task.isCollapsed()
              ? R.drawable.ic_keyboard_arrow_up_black_24dp
              : R.drawable.ic_keyboard_arrow_down_black_24dp,
          activity
              .getResources()
              .getQuantityString(R.plurals.subtask_count, task.children, task.children),
          0,
          true,
          true);
      chips.add(chip);
    }
    if (task.hasLocation()) {
      Chip chip = newChip(activity, task.getLocation());
      apply(
          activity, chip, R.drawable.ic_outline_place_24px, task.getLocation().getDisplayName(), 0, showText, showIcon);
      chips.add(chip);
    }
    if (!isSubtask) {
      if (!Strings.isNullOrEmpty(task.getGoogleTaskList()) && !(filter instanceof GtasksFilter)) {
        chips.add(
            newChip(
                activity,
                googleTaskLists.get(task.getGoogleTaskList()),
                R.drawable.ic_outline_cloud_24px));
      } else if (!Strings.isNullOrEmpty(task.getCaldav()) && !(filter instanceof CaldavFilter)) {
        chips.add(
            newChip(
                activity, caldavCalendars.get(task.getCaldav()), R.drawable.ic_outline_cloud_24px));
      }
    }
    String tagString = task.getTagsString();
    if (!Strings.isNullOrEmpty(tagString)) {
      Set<String> tags = newHashSet(tagString.split(","));
      if (filter instanceof TagFilter) {
        tags.remove(((TagFilter) filter).getUuid());
      }
      chips.addAll(
          transform(
              orderByName.sortedCopy(filter(transform(tags, tagDatas::get), Predicates.notNull())),
              tag -> newChip(activity, tag, R.drawable.ic_outline_label_24px)));
    }

    removeIf(chips, Predicates.isNull());
    return chips;
  }

  public void apply(Chip chip, Filter filter) {
    apply(
        chip.getContext(),
        chip,
        getIcon(filter.icon, R.drawable.ic_outline_cloud_24px),
        filter.listingTitle,
        filter.tint,
        true,
        true);
  }

  public void apply(Chip chip, @NonNull TagData tagData) {
    apply(
        chip.getContext(),
        chip,
        getIcon(tagData.getIcon(), R.drawable.ic_outline_label_24px),
        tagData.getName(),
        tagData.getColor(),
        true,
        true);
  }

  private @Nullable Chip newChip(Activity activity, Filter filter, int defIcon) {
    return newChip(activity, filter, defIcon, showText, showIcon);
  }

  Chip newChip(Activity activity, Filter filter, int defIcon, boolean showText, boolean showIcon) {
    if (filter == null) {
      return null;
    }
    Chip chip = newChip(activity, filter);
    apply(activity, chip, getIcon(filter.icon, defIcon), filter.listingTitle, filter.tint, showText, showIcon);
    return chip;
  }

  public Chip newClosableChip(Activity activity, Object tag) {
    Chip chip = getChip(activity);
    chip.setCloseIconVisible(true);
    chip.setTag(tag);
    return chip;
  }

  private Chip newChip(Activity activity, Object tag) {
    Chip chip = getChip(activity);
    chip.setTag(tag);
    return chip;
  }

  private Chip getChip(Activity activity) {
    return (Chip)
        activity
            .getLayoutInflater()
            .inflate(filled ? R.layout.chip_filled : R.layout.chip_outlined, null);
  }

  private void apply(
      Context context,
      Chip chip,
      @Nullable @DrawableRes Integer icon,
      String name,
      int theme,
      boolean showText,
      boolean showIcon) {
    if (showText) {
      chip.setText(name);
      chip.setIconEndPadding(0f);
    } else {
      chip.setText(null);
      chip.setContentDescription(name);
      chip.setTextStartPadding(0f);
      chip.setChipEndPadding(0f);
    }
    ThemeColor themeColor = getColor(context, theme);
    if (themeColor != null) {
      int primaryColor = themeColor.getPrimaryColor();
      ColorStateList primaryColorSL =
          new ColorStateList(new int[][] {new int[] {}}, new int[] {primaryColor});
      if (filled) {
        int colorOnPrimary = themeColor.getColorOnPrimary();
        ColorStateList colorOnPrimarySL =
            new ColorStateList(new int[][] {new int[] {}}, new int[] {colorOnPrimary});
        chip.setChipBackgroundColor(primaryColorSL);
        chip.setTextColor(colorOnPrimary);
        chip.setCloseIconTint(colorOnPrimarySL);
        chip.setChipIconTint(colorOnPrimarySL);
      } else {
        chip.setTextColor(primaryColor);
        chip.setCloseIconTint(primaryColorSL);
        chip.setChipIconTint(primaryColorSL);
        chip.setChipStrokeColor(primaryColorSL);
      }
    }
    if (showIcon && icon != null) {
      chip.setChipIconResource(icon);
      chip.getChipDrawable().setAlpha(iconAlpha);
    }
  }

  private @DrawableRes Integer getIcon(int index, int def) {
    Integer icon = CustomIcons.getIconResId(index);
    return icon != null ? icon : def;
  }

  private @Nullable ThemeColor getColor(Context context, int theme) {
    if (theme != 0) {
      ThemeColor color = newThemeColor(context, theme);
      if (color.isFree() || inventory.purchasedThemes()) {
        return color;
      }
    }
    return null;
  }
}
