package org.tasks.preferences;

import static com.todoroo.astrid.core.BuiltInFilterExposer.getMyTasksFilter;
import static com.todoroo.astrid.core.BuiltInFilterExposer.getRecentlyModifiedFilter;
import static com.todoroo.astrid.core.BuiltInFilterExposer.getTodayFilter;
import static com.todoroo.astrid.core.BuiltInFilterExposer.isRecentlyModifiedFilter;
import static com.todoroo.astrid.core.BuiltInFilterExposer.isTodayFilter;
import static org.tasks.Strings.isNullOrEmpty;

import android.content.Context;
import android.content.res.Resources;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.core.CustomFilterExposer;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.LocationDao;
import org.tasks.data.Place;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.filters.PlaceFilter;
import org.tasks.injection.ForApplication;
import timber.log.Timber;

public class DefaultFilterProvider {

  private static final int TYPE_FILTER = 0;
  private static final int TYPE_CUSTOM_FILTER = 1;
  private static final int TYPE_TAG = 2;
  private static final int TYPE_GOOGLE_TASKS = 3;
  private static final int TYPE_CALDAV = 4;
  private static final int TYPE_LOCATION = 5;

  private static final int FILTER_MY_TASKS = 0;
  private static final int FILTER_TODAY = 1;
  private static final int FILTER_UNCATEGORIZED = 2;
  private static final int FILTER_RECENTLY_MODIFIED = 3;

  private final Context context;
  private final Preferences preferences;
  private final CustomFilterExposer customFilterExposer;
  private final TagDataDao tagDataDao;
  private final GoogleTaskListDao googleTaskListDao;
  private final CaldavDao caldavDao;
  private final LocationDao locationDao;

  @Inject
  public DefaultFilterProvider(
      @ForApplication Context context,
      Preferences preferences,
      CustomFilterExposer customFilterExposer,
      TagDataDao tagDataDao,
      GoogleTaskListDao googleTaskListDao,
      CaldavDao caldavDao,
      LocationDao locationDao) {
    this.context = context;
    this.preferences = preferences;
    this.customFilterExposer = customFilterExposer;
    this.tagDataDao = tagDataDao;
    this.googleTaskListDao = googleTaskListDao;
    this.caldavDao = caldavDao;
    this.locationDao = locationDao;
  }

  public Filter getDashclockFilter() {
    return getFilterFromPreference(R.string.p_dashclock_filter);
  }

  public void setDashclockFilter(Filter filter) {
    setFilterPreference(filter, R.string.p_dashclock_filter);
  }

  public Filter getBadgeFilter() {
    return getFilterFromPreference(R.string.p_badge_list);
  }

  public void setBadgeFilter(Filter filter) {
    setFilterPreference(filter, R.string.p_badge_list);
  }

  public Filter getDefaultFilter() {
    return getFilterFromPreference(R.string.p_default_list);
  }

  public void setDefaultFilter(Filter filter) {
    setFilterPreference(filter, R.string.p_default_list);
  }

  public void setLastViewedFilter(Filter filter) {
    setFilterPreference(filter, R.string.p_last_viewed_list);
  }

  public Filter getDefaultRemoteList() {
    return getFilterFromPreference(
        preferences.getStringValue(R.string.p_default_remote_list), null);
  }

  public void setDefaultRemoteList(Filter filter) {
    setFilterPreference(filter, R.string.p_default_remote_list);
  }

  public Filter getStartupFilter() {
    return getFilterFromPreference(
        preferences.getBoolean(R.string.p_open_last_viewed_list, true)
            ? R.string.p_last_viewed_list
            : R.string.p_default_list);
  }

  public Filter getFilterFromPreference(int resId) {
    return getFilterFromPreference(preferences.getStringValue(resId));
  }

  public Filter getFilterFromPreference(String preferenceValue) {
    return getFilterFromPreference(
        preferenceValue, BuiltInFilterExposer.getMyTasksFilter(context.getResources()));
  }

  private Filter getFilterFromPreference(String preferenceValue, Filter def) {
    if (!isNullOrEmpty(preferenceValue)) {
      try {
        Filter filter = loadFilter(preferenceValue);
        if (filter != null) {
          return filter;
        }
      } catch (Exception e) {
        Timber.e(e);
      }
    }
    return def;
  }

  private Filter loadFilter(String preferenceValue) {
    String[] split = preferenceValue.split(":");
    switch (Integer.parseInt(split[0])) {
      case TYPE_FILTER:
        return getBuiltInFilter(Integer.parseInt(split[1]));
      case TYPE_CUSTOM_FILTER:
        return customFilterExposer.getFilter(Long.parseLong(split[1]));
      case TYPE_TAG:
        TagData tag = tagDataDao.getByUuid(split[1]);
        return tag == null || isNullOrEmpty(tag.getName()) ? null : new TagFilter(tag);
      case TYPE_GOOGLE_TASKS:
        GoogleTaskList list = googleTaskListDao.getById(Long.parseLong(split[1]));
        return list == null ? null : new GtasksFilter(list);
      case TYPE_CALDAV:
        CaldavCalendar caldavCalendar = caldavDao.getCalendarByUuid(split[1]);
        return caldavCalendar == null ? null : new CaldavFilter(caldavCalendar);
      case TYPE_LOCATION:
        Place place = locationDao.getPlace(split[1]);
        return place == null ? null : new PlaceFilter(place);
      default:
        return null;
    }
  }

  private void setFilterPreference(Filter filter, int prefId) {
    String filterPreferenceValue = getFilterPreferenceValue(filter);
    if (!isNullOrEmpty(filterPreferenceValue)) {
      preferences.setString(prefId, filterPreferenceValue);
    }
  }

  public String getFilterPreferenceValue(Filter filter) {
    int filterType = getFilterType(filter);
    switch (filterType) {
      case TYPE_FILTER:
        return getFilterPreference(filterType, getBuiltInFilterId(filter));
      case TYPE_CUSTOM_FILTER:
        return getFilterPreference(filterType, ((CustomFilter) filter).getId());
      case TYPE_TAG:
        return getFilterPreference(filterType, ((TagFilter) filter).getUuid());
      case TYPE_GOOGLE_TASKS:
        return getFilterPreference(filterType, ((GtasksFilter) filter).getStoreId());
      case TYPE_CALDAV:
        return getFilterPreference(filterType, ((CaldavFilter) filter).getUuid());
      case TYPE_LOCATION:
        return getFilterPreference(filterType, ((PlaceFilter) filter).getUid());
    }
    return null;
  }

  private <T> String getFilterPreference(int type, T value) {
    return String.format("%s:%s", type, value);
  }

  private int getFilterType(Filter filter) {
    if (filter instanceof TagFilter) {
      return TYPE_TAG;
    } else if (filter instanceof GtasksFilter) {
      return TYPE_GOOGLE_TASKS;
    } else if (filter instanceof CustomFilter) {
      return TYPE_CUSTOM_FILTER;
    } else if (filter instanceof CaldavFilter) {
      return TYPE_CALDAV;
    } else if (filter instanceof PlaceFilter) {
      return TYPE_LOCATION;
    }
    return TYPE_FILTER;
  }

  private Filter getBuiltInFilter(int id) {
    Resources resources = context.getResources();
    switch (id) {
      case FILTER_TODAY:
        return getTodayFilter(resources);
      case FILTER_RECENTLY_MODIFIED:
        return getRecentlyModifiedFilter(resources);
      case FILTER_UNCATEGORIZED:
        break;
    }
    return getMyTasksFilter(resources);
  }

  private int getBuiltInFilterId(Filter filter) {
    if (isTodayFilter(context, filter)) {
      return FILTER_TODAY;
    } else if (isRecentlyModifiedFilter(context, filter)) {
      return FILTER_RECENTLY_MODIFIED;
    }

    return FILTER_MY_TASKS;
  }
}
