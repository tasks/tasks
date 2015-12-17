package org.tasks.filters;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.BuiltInFilters;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.gtasks.GtasksFilterExposer;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.timers.TimerFilterExposer;

import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class FilterProvider {
    private final BuiltInFilters builtInFilters;
    private final TimerFilterExposer timerFilterExposer;
    private final CustomFilterExposer customFilterExposer;
    private final TagFilterExposer tagFilterExposer;
    private final GtasksFilterExposer gtasksFilterExposer;
    private final Preferences preferences;

    @Inject
    public FilterProvider(BuiltInFilters builtInFilters, TimerFilterExposer timerFilterExposer,
                          CustomFilterExposer customFilterExposer, TagFilterExposer tagFilterExposer,
                          GtasksFilterExposer gtasksFilterExposer, Preferences preferences) {

        this.builtInFilters = builtInFilters;
        this.timerFilterExposer = timerFilterExposer;
        this.customFilterExposer = customFilterExposer;
        this.tagFilterExposer = tagFilterExposer;
        this.gtasksFilterExposer = gtasksFilterExposer;
        this.preferences = preferences;
    }

    public Filter getMyTasksFilter() {
        return builtInFilters.getMyTasks();
    }

    public List<Filter> getFilters() {
        return new ArrayList<Filter>() {{
            if (preferences.getBoolean(R.string.p_show_today_filter, true)) {
                add(builtInFilters.getToday());
            }
            if (preferences.getBoolean(R.string.p_show_recently_modified_filter, true)) {
                add(builtInFilters.getRecentlyModified());
            }
            if (preferences.getBoolean(R.string.p_show_not_in_list_filter, true)) {
                add(builtInFilters.getUncategorized());
            }
            addAll(timerFilterExposer.getFilters());
            addAll(customFilterExposer.getFilters());
        }};
    }

    public List<Filter> getTags() {
        return tagFilterExposer.getFilters();
    }

    public List<Filter> getGoogleTaskFilters() {
        return gtasksFilterExposer.getFilters();
    }
}
