package org.tasks.caldav;

import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.sync.SyncAdapters;

public class CalDAVFilterExposer {

  private final SyncAdapters syncAdapters;
  private final CaldavDao caldavDao;

  @Inject
  public CalDAVFilterExposer(CaldavDao caldavDao, SyncAdapters syncAdapters) {
    this.caldavDao = caldavDao;
    this.syncAdapters = syncAdapters;
  }

  public List<Filter> getFilters() {
    if (!syncAdapters.isCaldavSyncEnabled()) {
      return Collections.emptyList();
    }
    List<CaldavAccount> allOrderedByName = caldavDao.getAllOrderedByName();
    List<Filter> result = new ArrayList<>();
    for (CaldavAccount account : allOrderedByName) {
      result.add(new CaldavFilter(account));
    }
    return result;
  }

  public Filter getFilterByUuid(String uuid) {
    if (syncAdapters.isCaldavSyncEnabled()) {
      CaldavAccount caldavAccount = caldavDao.getByUuid(uuid);
      if (caldavAccount != null) {
        return new CaldavFilter(caldavAccount);
      }
    }
    return null;
  }
}
