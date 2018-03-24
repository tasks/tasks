package org.tasks.filters;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.gtasks.GtasksFilterExposer;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.timers.TimerFilterExposer;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.caldav.CalDAVFilterExposer;

public class FilterProvider {

  private final BuiltInFilterExposer builtInFilterExposer;
  private final TimerFilterExposer timerFilterExposer;
  private final CustomFilterExposer customFilterExposer;
  private final TagFilterExposer tagFilterExposer;
  private final GtasksFilterExposer gtasksFilterExposer;
  private final CalDAVFilterExposer calDAVFilterExposer;

  @Inject
  public FilterProvider(BuiltInFilterExposer builtInFilterExposer,
      TimerFilterExposer timerFilterExposer,
      CustomFilterExposer customFilterExposer, TagFilterExposer tagFilterExposer,
      GtasksFilterExposer gtasksFilterExposer, CalDAVFilterExposer calDAVFilterExposer) {
    this.builtInFilterExposer = builtInFilterExposer;
    this.timerFilterExposer = timerFilterExposer;
    this.customFilterExposer = customFilterExposer;
    this.tagFilterExposer = tagFilterExposer;
    this.gtasksFilterExposer = gtasksFilterExposer;
    this.calDAVFilterExposer = calDAVFilterExposer;
  }

  public Filter getMyTasksFilter() {
    return builtInFilterExposer.getMyTasksFilter();
  }

  public List<Filter> getFilters() {
    ArrayList<Filter> filters = new ArrayList<>();
    filters.addAll(builtInFilterExposer.getFilters());
    filters.addAll(timerFilterExposer.getFilters());
    filters.addAll(customFilterExposer.getFilters());
    return filters;
  }

  public List<Filter> getTags() {
    return tagFilterExposer.getFilters();
  }

  public List<Filter> getGoogleTaskFilters() {
    return gtasksFilterExposer.getFilters();
  }

  public List<Filter> getCalDAVFilters() {
    return calDAVFilterExposer.getFilters();
  }
}
