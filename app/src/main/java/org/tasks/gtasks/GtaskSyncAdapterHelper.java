package org.tasks.gtasks;

import javax.inject.Inject;
import org.tasks.data.GoogleTaskListDao;

public class GtaskSyncAdapterHelper {

  private final GoogleTaskListDao googleTaskListDao;
  private final GoogleAccountManager googleAccountManager;

  @Inject
  public GtaskSyncAdapterHelper(
      GoogleTaskListDao googleTaskListDao, GoogleAccountManager googleAccountManager) {
    this.googleTaskListDao = googleTaskListDao;
    this.googleAccountManager = googleAccountManager;
  }

  public boolean isEnabled() {
    return !googleTaskListDao.getAccounts().isEmpty()
        && !googleAccountManager.getAccounts().isEmpty();
  }
}
