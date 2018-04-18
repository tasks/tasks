/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.sync.SyncResultCallback;
import javax.inject.Inject;
import org.tasks.data.GoogleTaskList;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.sync.SyncExecutor;

/**
 * SyncV2Service is a simplified synchronization interface for supporting next-generation sync
 * interfaces such as Google Tasks and Astrid.com
 *
 * @author Tim Su <tim@astrid.com>
 */
public class SyncV2Service {

  /*
   * At present, sync provider interactions are handled through code. If
   * there is enough interest, the Astrid team could create an interface
   * for responding to sync requests through this new API.
   */
  private final SyncExecutor syncExecutor;
  private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
  private final GtasksSyncService gtasksSyncService;

  @Inject
  public SyncV2Service(
      SyncExecutor syncExecutor,
      GtaskSyncAdapterHelper gtaskSyncAdapterHelper,
      GtasksSyncService gtasksSyncService) {
    this.syncExecutor = syncExecutor;
    this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
    this.gtasksSyncService = gtasksSyncService;
  }

  public void clearCompleted(final GoogleTaskList list, final SyncResultCallback callback) {
    if (gtaskSyncAdapterHelper.isEnabled()) {
      syncExecutor.execute(
          callback,
          () -> {
            callback.started();
            try {
              gtasksSyncService.clearCompleted(list);
            } finally {
              callback.finished();
            }
          });
    }
  }
}
