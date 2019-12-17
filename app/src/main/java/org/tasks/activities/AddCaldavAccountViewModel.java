package org.tasks.activities;

import android.content.Context;
import org.tasks.caldav.CaldavClient;
import org.tasks.gtasks.PlayServices;
import org.tasks.ui.CompletableViewModel;

public class AddCaldavAccountViewModel extends CompletableViewModel<String> {
  public void addAccount(
      PlayServices playServices,
      Context context,
      CaldavClient client,
      String url,
      String username,
      String password) {
    run(
        () -> {
          playServices.updateSecurityProvider(context);
          return client.forUrl(url, username, password).getHomeSet();
        });
  }
}
