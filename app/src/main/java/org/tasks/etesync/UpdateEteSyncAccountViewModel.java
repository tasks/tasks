package org.tasks.etesync;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import com.etesync.journalmanager.UserInfoManager.UserInfo;
import com.google.common.base.Strings;
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
          EteSyncClient newClient = client.setForeground().forUrl(url, username, null, token);
          return Strings.isNullOrEmpty(password)
              ? Pair.create(newClient.getUserInfo(), token)
              : newClient.getInfoAndToken(password);
        });
  }
}
