package org.tasks.etesync;

import static org.tasks.Strings.isNullOrEmpty;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import com.etesync.journalmanager.UserInfoManager.UserInfo;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class UpdateEteSyncAccountViewModel extends CompletableViewModel<Pair<UserInfo, String>> {
  void updateAccount(
      EteSyncClient client,
      String url,
      String username,
      @Nullable String password,
      @Nullable String token) {
    run(
        () -> {
          client.setForeground();
          if (isNullOrEmpty(password)) {
            return Pair.create(client.forUrl(url, username, null, token).getUserInfo(), token);
          } else {
            String newToken = client.forUrl(url, username, null, null).getToken(password);
            return Pair.create(
                client.forUrl(url, username, null, newToken).getUserInfo(), newToken);
          }
        });
  }
}
