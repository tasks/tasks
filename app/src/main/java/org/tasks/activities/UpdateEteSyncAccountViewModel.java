package org.tasks.activities;

import android.content.Context;
import androidx.core.util.Pair;
import org.tasks.etesync.EteSyncClient;
import org.tasks.ui.CompletableViewModel;

public class UpdateEteSyncAccountViewModel extends CompletableViewModel<Pair<String, String>> {
  public void addAccount(
      Context context,
      EteSyncClient client,
      String url,
      String username,
      String password,
      String encryptionPassword) {
    run(
        () ->
            client
                .setForeground()
                .forUrl(url, username, encryptionPassword, null)
                .getKeyAndToken(password));
  }
}
