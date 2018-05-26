package org.tasks.caldav;

import static com.google.common.collect.Lists.transform;

import android.support.v4.util.Pair;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.sync.SyncAdapters;

public class CaldavFilterExposer {

  private final SyncAdapters syncAdapters;
  private final CaldavDao caldavDao;

  @Inject
  public CaldavFilterExposer(CaldavDao caldavDao, SyncAdapters syncAdapters) {
    this.caldavDao = caldavDao;
    this.syncAdapters = syncAdapters;
  }

  public List<Pair<CaldavAccount, List<Filter>>> getFilters() {
    List<Pair<CaldavAccount, List<Filter>>> filters = new ArrayList<>();
    for (CaldavAccount account : caldavDao.getAccounts()) {
      List<CaldavCalendar> calendars = caldavDao.getCalendarsByAccount(account.getUuid());
      filters.add(new Pair<>(account, transform(calendars, CaldavFilter::new)));
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
