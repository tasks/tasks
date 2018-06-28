/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

import android.support.v4.util.Pair;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
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

  public List<Pair<GoogleTaskAccount, List<Filter>>> getFilters() {
    List<Pair<GoogleTaskAccount, List<Filter>>> listFilters = newArrayList();
    for (GoogleTaskAccount account : googleTaskListDao.getAccounts()) {
      List<GoogleTaskList> lists = googleTaskListDao.getLists(account.getAccount());
      listFilters.add(new Pair<>(account, transform(lists, GtasksFilter::new)));
    }
    return listFilters;
  }

  public Filter getFilter(long id) {
    if (syncAdapters.isGoogleTaskSyncEnabled()) {
      GoogleTaskList list = gtasksListService.getList(id);
      if (list != null) {
        return filterFromList(list);
      }
    }
    return null;
  }

  private Filter filterFromList(GoogleTaskList list) {
    return new GtasksFilter(list);
  }
}
