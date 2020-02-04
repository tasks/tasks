package org.tasks.etesync;

import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.ui.ActionViewModel;

@SuppressWarnings("WeakerAccess")
public class DeleteCalendarViewModel extends ActionViewModel {
  void deleteCalendar(EteSyncClient client, CaldavAccount account, CaldavCalendar calendar) {
    run(() -> client.forAccount(account).deleteCollection(calendar));
  }
}
