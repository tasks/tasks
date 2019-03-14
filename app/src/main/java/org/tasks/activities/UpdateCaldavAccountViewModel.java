package org.tasks.activities;

import org.tasks.caldav.CaldavClient;
import org.tasks.ui.CompletableViewModel;

public class UpdateCaldavAccountViewModel extends CompletableViewModel<String> {
  public void updateCaldavAccount(String url, String username, String password) {
    run(() -> new CaldavClient(url, username, password).getHomeSet());
  }
}
