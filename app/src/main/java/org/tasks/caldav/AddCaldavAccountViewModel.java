package org.tasks.caldav;

import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class AddCaldavAccountViewModel extends CompletableViewModel<String> {
  void addAccount(CaldavClient client, String url, String username, String password) {
    run(
        () -> client.setForeground().forUrl(url, username, password).getHomeSet());
  }
}
