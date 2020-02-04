package org.tasks.caldav;

import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class UpdateCaldavAccountViewModel extends CompletableViewModel<String> {
  void updateCaldavAccount(
      CaldavClient client, String url, String username, String password) {
    run(() -> client.forUrl(url, username, password).getHomeSet());
  }
}
