package org.tasks.etesync;

import androidx.core.util.Pair;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class UpdateEteSyncAccountViewModel extends CompletableViewModel<Pair<String, String>> {
  void updateAccount(
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
