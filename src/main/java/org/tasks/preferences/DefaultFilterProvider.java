package org.tasks.preferences;

import android.content.Context;
import android.content.res.Resources;

import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.gtasks.GtasksFilterExposer;
import com.todoroo.astrid.gtasks.GtasksListFragment;
import com.todoroo.astrid.tags.TagFilterExposer;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.injection.ForApplication;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.todoroo.astrid.core.BuiltInFilterExposer.getMyTasksFilter;
import static com.todoroo.astrid.core.BuiltInFilterExposer.getRecentlyModifiedFilter;
import static com.todoroo.astrid.core.BuiltInFilterExposer.getTodayFilter;
import static com.todoroo.astrid.core.BuiltInFilterExposer.getUncategorizedFilter;
import static com.todoroo.astrid.core.BuiltInFilterExposer.isRecentlyModifiedFilter;
import static com.todoroo.astrid.core.BuiltInFilterExposer.isTodayFilter;
import static com.todoroo.astrid.core.BuiltInFilterExposer.isUncategorizedFilter;

public class DefaultFilterProvider {

    private static final int TYPE_FILTER = 0;
    private static final int TYPE_CUSTOM_FILTER = 1;
    private static final int TYPE_TAG = 2;
    private static final int TYPE_GOOGLE_TASKS = 3;

    private static final int FILTER_MY_TASKS = 0;
    private static final int FILTER_TODAY = 1;
    private static final int FILTER_UNCATEGORIZED = 2;
    private static final int FILTER_RECENTLY_MODIFIED = 3;

    private final Context context;
    private final Preferences preferences;
    private final Tracker tracker;
    private final CustomFilterExposer customFilterExposer;
    private final TagFilterExposer tagFilterExposer;
    private final GtasksFilterExposer gtasksFilterExposer;

    @Inject
    public DefaultFilterProvider(@ForApplication Context context, Preferences preferences,
                                 Tracker tracker, CustomFilterExposer customFilterExposer,
                                 TagFilterExposer tagFilterExposer, GtasksFilterExposer gtasksFilterExposer) {
        this.context = context;
        this.preferences = preferences;
        this.tracker = tracker;
        this.customFilterExposer = customFilterExposer;
        this.tagFilterExposer = tagFilterExposer;
        this.gtasksFilterExposer = gtasksFilterExposer;
    }

    public Filter getDefaultFilter() {
        String defaultList = preferences.getStringValue(R.string.p_default_list);
        if (!isNullOrEmpty(defaultList)) {
            try {
                Filter filter = loadFilterFromPreferences(defaultList);
                if (filter != null) {
                    return filter;
                }
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }
        }
        return BuiltInFilterExposer.getMyTasksFilter(context.getResources());
    }

    private Filter loadFilterFromPreferences(String defaultList) {
        String[] split = defaultList.split(":");
        switch (Integer.parseInt(split[0])) {
            case TYPE_FILTER:
                return getBuiltInFilter(Integer.parseInt(split[1]));
            case TYPE_CUSTOM_FILTER:
                return customFilterExposer.getFilter(Long.parseLong(split[1]));
            case TYPE_TAG:
                return tagFilterExposer.getFilterByUuid(split[1]);
            case TYPE_GOOGLE_TASKS:
                return gtasksFilterExposer.getFilter(Long.parseLong(split[1]));
            default:
                return null;
        }
    }

    public void setDefaultFilter(Filter filter) {
        tracker.reportEvent(Tracking.Events.SET_DEFAULT_LIST);
        int filterType = getFilterType(filter);
        switch (filterType) {
            case TYPE_FILTER:
                setDefaultFilter(filterType, getBuiltInFilterId(filter));
                break;
            case TYPE_CUSTOM_FILTER:
                setDefaultFilter(filterType, ((CustomFilter) filter).getId());
                break;
            case TYPE_TAG:
                setDefaultFilter(filterType, ((FilterWithCustomIntent) filter).customExtras.getString(TagViewFragment.EXTRA_TAG_UUID));
                break;
            case TYPE_GOOGLE_TASKS:
                setDefaultFilter(filterType, ((FilterWithCustomIntent) filter).customExtras.getLong(GtasksListFragment.TOKEN_STORE_ID, 0L));
                break;
        }
    }

    private <T> void setDefaultFilter(int type, T value) {
        preferences.setString(R.string.p_default_list, String.format("%s:%s", type, value));
    }

    private int getFilterType(Filter filter) {
        if (filter instanceof FilterWithCustomIntent) {
            return filter.isTagFilter()
                    ? TYPE_TAG
                    : TYPE_GOOGLE_TASKS;
        } else if (filter instanceof CustomFilter) {
            return TYPE_CUSTOM_FILTER;
        }
        return TYPE_FILTER;
    }

    private Filter getBuiltInFilter(int id) {
        Resources resources = context.getResources();
        switch (id) {
            case FILTER_TODAY:
                return getTodayFilter(resources);
            case FILTER_UNCATEGORIZED:
                return getUncategorizedFilter(resources);
            case FILTER_RECENTLY_MODIFIED:
                return getRecentlyModifiedFilter(resources);
        }
        return getMyTasksFilter(resources);
    }

    private int getBuiltInFilterId(Filter filter) {
        if (isTodayFilter(context, filter)) {
            return FILTER_TODAY;
        } else if (isUncategorizedFilter(context, filter)) {
            return FILTER_UNCATEGORIZED;
        } else if (isRecentlyModifiedFilter(context, filter)) {
            return FILTER_RECENTLY_MODIFIED;
        }

        return FILTER_MY_TASKS;
    }
}
