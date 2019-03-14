package org.tasks.activities;

import org.tasks.caldav.CaldavClient;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.security.Encryption;
import org.tasks.ui.ActionViewModel;

public class DeleteCalendarViewModel extends ActionViewModel {
  public void deleteCalendar(
      CaldavAccount caldavAccount, Encryption encryption, CaldavCalendar calendar) {
    run(() -> new CaldavClient(caldavAccount, calendar, encryption).deleteCollection());
  }
}
