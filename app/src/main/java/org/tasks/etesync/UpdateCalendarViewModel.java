package org.tasks.etesync;

import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class UpdateCalendarViewModel extends CompletableViewModel<String> {
  void updateCalendar(
      EteSyncClient client,
      CaldavAccount account,
      CaldavCalendar calendar,
      String name,
      int color) {
    run(() -> client.forAccount(account).updateCollection(calendar, name, color));
  }
}
