package org.tasks.activities;

import org.tasks.caldav.CaldavClient;
import org.tasks.ui.CompletableViewModel;

public class UpdateCaldavAccountViewModel extends CompletableViewModel<String> {
  public void updateCaldavAccount(
      CaldavClient client, String url, String username, String password) {
    run(() -> client.forUrl(url, username, password).getHomeSet());
  }
}
