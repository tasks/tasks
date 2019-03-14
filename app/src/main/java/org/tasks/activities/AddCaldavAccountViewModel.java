package org.tasks.activities;

import org.tasks.caldav.CaldavClient;
import org.tasks.ui.CompletableViewModel;

public class AddCaldavAccountViewModel extends CompletableViewModel<String> {
  public void addAccount(String url, String username, String password) {
    run(() -> new CaldavClient(url, username, password).getHomeSet());
  }
}
