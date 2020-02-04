package org.tasks.caldav;

import android.os.Bundle;
import androidx.lifecycle.ViewModelProviders;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.injection.ActivityComponent;

public class CaldavCalendarSettingsActivity extends BaseCaldavCalendarSettingsActivity {

  @Inject CaldavClient client;

  private CreateCalendarViewModel createCalendarViewModel;
  private DeleteCalendarViewModel deleteCalendarViewModel;

  @Override
  protected int getLayout() {
    return R.layout.activity_caldav_calendar_settings;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    createCalendarViewModel = ViewModelProviders.of(this).get(CreateCalendarViewModel.class);
    deleteCalendarViewModel = ViewModelProviders.of(this).get(DeleteCalendarViewModel.class);

    createCalendarViewModel.observe(this, this::createSuccessful, this::requestFailed);
    deleteCalendarViewModel.observe(this, this::onDeleted, this::requestFailed);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  protected void createCalendar(CaldavAccount caldavAccount, String name) {
    createCalendarViewModel.createCalendar(client, caldavAccount, name);
  }

  @Override
  protected void deleteCalendar(CaldavAccount caldavAccount, CaldavCalendar caldavCalendar) {
    deleteCalendarViewModel.deleteCalendar(client, caldavAccount, caldavCalendar);
  }
}
