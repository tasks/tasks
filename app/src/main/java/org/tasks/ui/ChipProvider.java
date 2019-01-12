package org.tasks.ui;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static org.tasks.preferences.ResourceResolver.getDimen;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import com.google.android.material.chip.Chip;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.tags.TagService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.TagData;
import org.tasks.injection.ForActivity;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

public class ChipProvider {

  private final Map<String, GtasksFilter> googleTaskLists = new HashMap<>();
  private final Map<String, CaldavFilter> caldavCalendars = new HashMap<>();
  private final Map<String, TagFilter> tagDatas = new HashMap<>();
  private final Context context;
  private final ThemeCache themeCache;
  private final int iconAlpha;
  private final GoogleTaskListDao googleTaskListDao;
  private final CaldavDao caldavDao;
  private final TagService tagService;
  private final Ordering<TagFilter> orderByName =
      new Ordering<TagFilter>() {
        @Override
        public int compare(TagFilter left, TagFilter right) {
          return left.listingTitle.compareTo(right.listingTitle);
        }
      };

  @Inject
  public ChipProvider(
      @ForActivity Context context,
      ThemeCache themeCache,
      GoogleTaskListDao googleTaskListDao,
      CaldavDao caldavDao,
      TagService tagService) {
    this.context = context;
    this.themeCache = themeCache;
    this.googleTaskListDao = googleTaskListDao;
    this.caldavDao = caldavDao;
    this.tagService = tagService;
    iconAlpha = (int) (255 * getDimen(context, R.dimen.alpha_secondary));
  }

  public Chip getChip(TagData tagData) {
    Chip chip = new Chip(context);
    chip.setCloseIconVisible(true);
    apply(chip, tagData.getName(), tagData.getColor());
    return chip;
  }

  public List<Chip> getChips(String caldav, String googleTask, Iterable<String> tagUuids) {
    List<Chip> chips = new ArrayList<>();
    if (!Strings.isNullOrEmpty(googleTask)) {
      GtasksFilter googleTaskFilter = getGoogleTaskList(googleTask);
      if (googleTaskFilter != null) {
        chips.add(newChip(googleTaskFilter));
      }
    } else if (!Strings.isNullOrEmpty(caldav)) {
      CaldavFilter caldavFilter = getCaldavCalendar(caldav);
      if (caldavFilter != null) {
        chips.add(newChip(caldavFilter));
      }
    }
    Iterable<TagFilter> tagFilters =
        filter(transform(tagUuids, this::getTag), Predicates.notNull());
    for (TagFilter tagFilter : orderByName.sortedCopy(tagFilters)) {
      chips.add(newChip(tagFilter));
    }

    return chips;
  }

  public void apply(Chip chip, Filter filter) {
    apply(chip, filter.listingTitle, filter.tint);
  }

  private Chip newChip(Filter filter) {
    LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
    Chip chip = (Chip) layoutInflater.inflate(R.layout.chip_task_list, null);
    chip.setTag(filter);
    apply(chip, filter.listingTitle, filter.tint);
    return chip;
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

  private GtasksFilter getGoogleTaskList(String remoteId) {
    GtasksFilter gtasksFilter = googleTaskLists.get(remoteId);
    if (gtasksFilter == null) {
      GoogleTaskList googleTaskList = googleTaskListDao.getByRemoteId(remoteId);
      if (googleTaskList != null) {
        gtasksFilter = new GtasksFilter(googleTaskList);
        googleTaskLists.put(remoteId, gtasksFilter);
      }
    }
    return gtasksFilter;
  }

  private CaldavFilter getCaldavCalendar(String uuid) {
    CaldavFilter caldavFilter = caldavCalendars.get(uuid);
    if (caldavFilter == null) {
      CaldavCalendar calendar = caldavDao.getCalendar(uuid);
      if (calendar != null) {
        caldavFilter = new CaldavFilter(calendar);
        caldavCalendars.put(uuid, caldavFilter);
      }
    }
    return caldavFilter;
  }

  private TagFilter getTag(String uuid) {
    TagFilter tagFilter = tagDatas.get(uuid);
    if (tagFilter == null) {
      TagData tagData = tagService.getTagByUuid(uuid);
      if (tagData != null) {
        tagFilter = new TagFilter(tagData);
        tagDatas.put(uuid, tagFilter);
      }
    }
    return tagFilter;
  }
}
