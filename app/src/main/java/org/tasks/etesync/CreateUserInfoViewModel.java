package org.tasks.etesync;

import org.tasks.data.CaldavAccount;
import org.tasks.ui.CompletableViewModel;

public class CreateUserInfoViewModel extends CompletableViewModel<String> {

  void createUserInfo(EteSyncClient client, CaldavAccount caldavAccount, String derivedKey) {
    run(() -> {
      client.forAccount(caldavAccount).createUserInfo(derivedKey);
      return derivedKey;
    });
  }
}
