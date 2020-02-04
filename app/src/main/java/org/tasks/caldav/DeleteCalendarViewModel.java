package org.tasks.caldav;

import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.ui.ActionViewModel;

@SuppressWarnings("WeakerAccess")
public class DeleteCalendarViewModel extends ActionViewModel {
  void deleteCalendar(CaldavClient client, CaldavAccount account, CaldavCalendar calendar) {
    run(() -> client.forCalendar(account, calendar).deleteCollection());
  }
}
