package org.tasks.gtasks;

import javax.inject.Inject;
import org.tasks.data.GoogleTaskListDao;

public class GtaskSyncAdapterHelper {

  private final PlayServices playServices;
  private final GoogleTaskListDao googleTaskListDao;

  @Inject
  public GtaskSyncAdapterHelper(PlayServices playServices, GoogleTaskListDao googleTaskListDao) {
    this.playServices = playServices;
    this.googleTaskListDao = googleTaskListDao;
  }

  public boolean isEnabled() {
    return !googleTaskListDao.getAccounts().isEmpty() && playServices.isPlayServicesAvailable();
  }
}
