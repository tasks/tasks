package org.tasks.caldav;

import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.ui.CompletableViewModel;

@SuppressWarnings("WeakerAccess")
public class UpdateCalendarViewModel extends CompletableViewModel<String> {
  void updateCalendar(
      CaldavClient client, CaldavAccount account, CaldavCalendar calendar, String name, int color) {
    run(() -> client.forCalendar(account, calendar).updateCollection(name, color));
  }
}
