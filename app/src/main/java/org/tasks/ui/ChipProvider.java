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
import androidx.annotation.LayoutRes;
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
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

@ApplicationScope
public class ChipProvider {

  private final Map<String, GtasksFilter> googleTaskLists = new HashMap<>();
  private final Map<String, CaldavFilter> caldavCalendars = new HashMap<>();
  private final Map<String, TagFilter> tagDatas = new HashMap<>();
  private final Context context;
  private final Inventory inventory;
  private final ThemeCache themeCache;
  private final int iconAlpha;
  private final LocalBroadcastManager localBroadcastManager;
  private final Ordering<TagFilter> orderByName =
      new Ordering<TagFilter>() {
        @Override
        public int compare(TagFilter left, TagFilter right) {
          return left.listingTitle.compareTo(right.listingTitle);
        }
      };

  @Inject
  public ChipProvider(
      @ForApplication Context context,
      Inventory inventory,
      ThemeCache themeCache,
      GoogleTaskListDao googleTaskListDao,
      CaldavDao caldavDao,
      TagDataDao tagDataDao,
      LocalBroadcastManager localBroadcastManager) {
    this.context = context;
    this.inventory = inventory;
    this.themeCache = themeCache;
    this.localBroadcastManager = localBroadcastManager;
    iconAlpha =
        (int) (255 * ResourcesCompat.getFloat(context.getResources(), R.dimen.alpha_secondary));

    googleTaskListDao.subscribeToLists().observeForever(this::updateGoogleTaskLists);
    caldavDao.subscribeToCalendars().observeForever(this::updateCaldavCalendars);
    tagDataDao.subscribeToTags().observeForever(this::updateTags);
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
      chips.add(
          newIconChip(
              activity,
              task.isCollapsed()
                  ? R.drawable.ic_keyboard_arrow_up_black_24dp
                  : R.drawable.ic_keyboard_arrow_down_black_24dp,
              activity
                  .getResources()
                  .getQuantityString(R.plurals.subtask_count, task.children, task.children),
              task));
    }
    if (task.hasLocation()) {
      chips.add(
          newIconChip(
              activity,
              R.drawable.ic_outline_place_24px,
              task.getLocation().getDisplayName(),
              task.getLocation()));
    }
    if (!isSubtask) {
      if (!Strings.isNullOrEmpty(task.getGoogleTaskList()) && !(filter instanceof GtasksFilter)) {
        chips.add(newTagChip(activity, googleTaskLists.get(task.getGoogleTaskList())));
      } else if (!Strings.isNullOrEmpty(task.getCaldav()) && !(filter instanceof CaldavFilter)) {
        chips.add(newTagChip(activity, caldavCalendars.get(task.getCaldav())));
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
              tag -> newTagChip(activity, tag)));
    }

    removeIf(chips, Predicates.isNull());
    return chips;
  }

  public void apply(Chip chip, Filter filter) {
    apply(chip, filter.listingTitle, filter.tint);
  }

  public void apply(Chip chip, @NonNull TagData tagData) {
    apply(chip, tagData.getName(), tagData.getColor());
  }

  private Chip newIconChip(Activity activity, int icon, String text, Object tag) {
    Chip chip = newChip(activity, R.layout.chip_button, tag);
    chip.setChipIconResource(icon);
    chip.setText(text);
    return chip;
  }

  private @Nullable Chip newTagChip(Activity activity, Filter filter) {
    if (filter == null) {
      return null;
    }
    Chip chip = newChip(activity, R.layout.chip_tag, filter);
    apply(chip, filter.listingTitle, filter.tint);
    return chip;
  }

  public Chip newClosableChip(Activity activity, Object tag) {
    Chip chip = (Chip) activity.getLayoutInflater().inflate(R.layout.chip_closable, null);
    chip.setTag(tag);
    return chip;
  }

  private Chip newChip(Activity activity, @LayoutRes int layout, Object tag) {
    Chip chip = (Chip) activity.getLayoutInflater().inflate(layout, null);
    chip.setTag(tag);
    return chip;
  }

  private void apply(Chip chip, String name, int theme) {
    ThemeColor color = getColor(theme);
    chip.setText(name);
    chip.setCloseIconTint(
        new ColorStateList(new int[][] {new int[] {}}, new int[] {color.getColorOnPrimary()}));
    chip.setTextColor(color.getColorOnPrimary());
    chip.getChipDrawable().setAlpha(iconAlpha);
    chip.setChipBackgroundColor(
        new ColorStateList(
            new int[][] {
              new int[] {-android.R.attr.state_checked}, new int[] {android.R.attr.state_checked}
            },
            new int[] {color.getPrimaryColor(), color.getPrimaryColor()}));
  }

  private ThemeColor getColor(int theme) {
    if (theme != 0) {
      ThemeColor color = newThemeColor(context, theme);
      if (color.isFree() || inventory.purchasedThemes()) {
        return color;
      }
    }
    return themeCache.getUntaggedColor();
  }
}
