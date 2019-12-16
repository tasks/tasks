/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.gtasks;

import static com.todoroo.andlib.utility.DateUtilities.now;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.filters.AlphanumComparator;
import org.tasks.filters.GoogleTaskFilters;
import org.tasks.sync.SyncAdapters;

/**
 * Exposes filters based on lists
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class GtasksFilterExposer {

  private final GtasksListService gtasksListService;
  private final SyncAdapters syncAdapters;
  private final GoogleTaskListDao googleTaskListDao;

  @Inject
  public GtasksFilterExposer(
      GtasksListService gtasksListService,
      SyncAdapters syncAdapters,
      GoogleTaskListDao googleTaskListDao) {
    this.gtasksListService = gtasksListService;
    this.syncAdapters = syncAdapters;
    this.googleTaskListDao = googleTaskListDao;
  }

  public Map<GoogleTaskAccount, List<Filter>> getFilters() {
    List<GoogleTaskFilters> googleTaskFilters = googleTaskListDao.getGoogleTaskFilters(now());
    LinkedHashMap<GoogleTaskAccount, List<Filter>> filters = new LinkedHashMap<>();
    for (GoogleTaskFilters filter : googleTaskFilters) {
      if (!filters.containsKey(filter.googleTaskAccount)) {
        filters.put(filter.googleTaskAccount, new ArrayList<>());
      }
      if (filter.googleTaskList != null) {
        filters.get(filter.googleTaskAccount).add(filter.toGtasksFilter());
      }
    }
    for (Map.Entry<GoogleTaskAccount, List<Filter>> entry : filters.entrySet()) {
      Collections.sort(entry.getValue(), new AlphanumComparator());
    }
    return filters;
  }

  public Filter getFilter(long id) {
    if (syncAdapters.isGoogleTaskSyncEnabled()) {
      GoogleTaskList list = gtasksListService.getList(id);
      if (list != null) {
        return new GtasksFilter(list);
      }
    }
    return null;
  }
}
