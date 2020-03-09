package org.tasks.etesync;

import android.content.Context;
import androidx.core.util.Pair;
import com.etesync.journalmanager.UserInfoManager.UserInfo;
import org.tasks.gtasks.PlayServices;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class AddEteSyncAccountViewModel extends CompletableViewModel<Pair<UserInfo, String>> {
  void addAccount(
      PlayServices playServices,
      Context context,
      EteSyncClient client,
      String url,
      String username,
      String password) {
    run(
        () -> {
          client.setForeground();
          playServices.updateSecurityProvider(context);
          String token = client.forUrl(url, username, null, null).getToken(password);
          return Pair.create(client.forUrl(url, username, null, token).getUserInfo(), token);
        });
  }
}
