package org.tasks.caldav;

import android.content.Context;
import org.tasks.caldav.CaldavClient;
import org.tasks.gtasks.PlayServices;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class AddCaldavAccountViewModel extends CompletableViewModel<String> {
  void addAccount(
      PlayServices playServices,
      Context context,
      CaldavClient client,
      String url,
      String username,
      String password) {
    run(
        () -> {
          playServices.updateSecurityProvider(context);
          return client.setForeground().forUrl(url, username, password).getHomeSet();
        });
  }
}
