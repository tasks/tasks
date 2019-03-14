package org.tasks.activities;

import org.tasks.caldav.CaldavClient;
import org.tasks.data.CaldavAccount;
import org.tasks.security.Encryption;
import org.tasks.ui.CompletableViewModel;

public class CreateCalendarViewModel extends CompletableViewModel<String> {
  public void createCalendar(CaldavAccount caldavAccount, Encryption encryption, String name) {
    run(() -> new CaldavClient(caldavAccount, encryption).makeCollection(name));
  }
}
