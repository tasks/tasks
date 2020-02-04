package org.tasks.etesync;

import org.tasks.data.CaldavAccount;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class CreateCalendarViewModel extends CompletableViewModel<String> {
  void createCalendar(EteSyncClient client, CaldavAccount account, String name) {
    run(() -> client.forAccount(account).makeCollection(name));
  }
}
