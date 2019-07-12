package org.tasks.caldav;

import static com.todoroo.andlib.utility.DateUtilities.now;

import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.filters.CaldavFilters;
import org.tasks.sync.SyncAdapters;

public class CaldavFilterExposer {

  private final SyncAdapters syncAdapters;
  private final CaldavDao caldavDao;

  @Inject
  public CaldavFilterExposer(CaldavDao caldavDao, SyncAdapters syncAdapters) {
    this.caldavDao = caldavDao;
    this.syncAdapters = syncAdapters;
  }

  public Map<CaldavAccount, List<Filter>> getFilters() {
    List<CaldavFilters> caldavFilters = caldavDao.getCaldavFilters(now());
    LinkedHashMap<CaldavAccount, List<Filter>> filters = new LinkedHashMap<>();
    for (CaldavFilters filter : caldavFilters) {
      if (!filters.containsKey(filter.caldavAccount)) {
        filters.put(filter.caldavAccount, new ArrayList<>());
      }
      if (filter.caldavCalendar != null) {
        filters.get(filter.caldavAccount).add(filter.toCaldavFilter());
      }
    }
    return filters;
  }

  public Filter getFilterByUuid(String uuid) {
    if (syncAdapters.isCaldavSyncEnabled()) {
      CaldavCalendar caldavCalendar = caldavDao.getCalendarByUuid(uuid);
      if (caldavCalendar != null) {
        return new CaldavFilter(caldavCalendar);
      }
    }
    return null;
  }
}
