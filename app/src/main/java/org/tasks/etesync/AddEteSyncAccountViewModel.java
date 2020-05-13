package org.tasks.etesync;

import androidx.core.util.Pair;
import com.etesync.journalmanager.UserInfoManager.UserInfo;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class AddEteSyncAccountViewModel extends CompletableViewModel<Pair<UserInfo, String>> {
  void addAccount(EteSyncClient client, String url, String username, String password) {
    run(
        () -> {
          client.setForeground();
          String token = client.forUrl(url, username, null, null).getToken(password);
          return Pair.create(client.forUrl(url, username, null, token).getUserInfo(), token);
        });
  }
}
