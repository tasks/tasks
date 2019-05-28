package org.tasks.activities;

import org.tasks.caldav.CaldavClient;
import org.tasks.data.CaldavAccount;
import org.tasks.ui.CompletableViewModel;

public class CreateCalendarViewModel extends CompletableViewModel<String> {
  public void createCalendar(CaldavClient client, CaldavAccount account, String name) {
    run(() -> client.forAccount(account).makeCollection(name));
  }
}
