package org.tasks.filters;

import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.gtasks.GtasksFilterExposer;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.timers.TimerFilterExposer;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class FilterProvider {
    private final BuiltInFilterExposer builtInFilterExposer;
    private final TimerFilterExposer timerFilterExposer;
    private final CustomFilterExposer customFilterExposer;
    private final TagFilterExposer tagFilterExposer;
    private final GtasksFilterExposer gtasksFilterExposer;

    @Inject
    public FilterProvider(BuiltInFilterExposer builtInFilterExposer, TimerFilterExposer timerFilterExposer,
                          CustomFilterExposer customFilterExposer, TagFilterExposer tagFilterExposer,
                          GtasksFilterExposer gtasksFilterExposer) {

        this.builtInFilterExposer = builtInFilterExposer;
        this.timerFilterExposer = timerFilterExposer;
        this.customFilterExposer = customFilterExposer;
        this.tagFilterExposer = tagFilterExposer;
        this.gtasksFilterExposer = gtasksFilterExposer;
    }

    public List<? extends FilterListItem> getFilters() {
        return new ArrayList<FilterListItem>() {{
            addAll(builtInFilterExposer.getFilters());
            addAll(timerFilterExposer.getFilters());
            addAll(customFilterExposer.getFilters());
            addAll(tagFilterExposer.getFilters());
            addAll(gtasksFilterExposer.getFilters());
        }};
    }
}
