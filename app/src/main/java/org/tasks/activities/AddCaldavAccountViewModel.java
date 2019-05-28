package org.tasks.activities;

import org.tasks.caldav.CaldavClient;
import org.tasks.ui.CompletableViewModel;

public class AddCaldavAccountViewModel extends CompletableViewModel<String> {
  public void addAccount(CaldavClient client, String url, String username, String password) {
    run(() -> client.forUrl(url, username, password).getHomeSet());
  }
}
