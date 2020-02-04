package org.tasks.etesync;

import android.content.Context;
import androidx.core.util.Pair;
import com.etesync.journalmanager.Crypto.AsymmetricKeyPair;
import org.tasks.etesync.EteSyncClient;
import org.tasks.gtasks.PlayServices;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class AddEteSyncAccountViewModel
    extends CompletableViewModel<Pair<String, String>> {
  void addAccount(
      PlayServices playServices,
      Context context,
      EteSyncClient client,
      String url,
      String username,
      String password,
      String encryptionPassword) {
    run(
        () -> {
          playServices.updateSecurityProvider(context);
          return client
              .setForeground()
              .forUrl(url, username, encryptionPassword, null)
              .getKeyAndToken(password);
        });
  }
}
