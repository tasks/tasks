package org.tasks.activities;

import org.tasks.caldav.CaldavClient;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.ui.ActionViewModel;

public class DeleteCalendarViewModel extends ActionViewModel {
  public void deleteCalendar(CaldavClient client, CaldavAccount account, CaldavCalendar calendar) {
    run(() -> client.forCalendar(account, calendar).deleteCollection());
  }
}
