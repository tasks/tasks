/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.os.Bundle;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.service.TaskDeleter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.calendars.CalendarEventProvider;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.Preferences;
import org.tasks.ui.ProgressDialogAsyncTask;

public class OldTaskPreferences extends InjectingPreferenceActivity {

  @Inject DialogBuilder dialogBuilder;
  @Inject Preferences preferences;
  @Inject Database database;
  @Inject TaskDao taskDao;
  @Inject CalendarEventProvider calendarEventProvider;
  @Inject TaskDeleter taskDeleter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_oldtasks);

    findPreference(getString(R.string.EPr_manage_purge_deleted))
        .setOnPreferenceClickListener(
            preference -> {
              purgeDeletedTasks();
              return false;
            });

    findPreference(getString(R.string.EPr_manage_delete_completed_gcal))
        .setOnPreferenceClickListener(
            preference -> {
              deleteCompletedEvents();
              return false;
            });

    findPreference(getString(R.string.EPr_manage_delete_all_gcal))
        .setOnPreferenceClickListener(
            preference -> {
              deleteAllCalendarEvents();
              return false;
            });

    findPreference(getString(R.string.EPr_reset_preferences))
        .setOnPreferenceClickListener(
            preference -> {
              resetPreferences();
              return false;
            });

    findPreference(getString(R.string.EPr_delete_task_data))
        .setOnPreferenceClickListener(
            preference -> {
              deleteTaskData();
              return false;
            });
  }

  private void purgeDeletedTasks() {
    dialogBuilder
        .newMessageDialog(R.string.EPr_manage_purge_deleted_message)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) ->
                new ProgressDialogAsyncTask(OldTaskPreferences.this, dialogBuilder) {
                  @Override
                  protected Integer doInBackground(Void... params) {
                    return taskDeleter.purgeDeleted();
                  }

                  @Override
                  protected int getResultResource() {
                    return R.string.EPr_manage_purge_deleted_status;
                  }
                }.execute())
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void deleteCompletedEvents() {
    dialogBuilder
        .newMessageDialog(R.string.EPr_manage_delete_completed_gcal_message)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) ->
                new ProgressDialogAsyncTask(OldTaskPreferences.this, dialogBuilder) {

                  @Override
                  protected Integer doInBackground(Void... params) {
                    calendarEventProvider.deleteEvents(taskDao.getCompletedCalendarEvents());
                    return taskDao.clearCompletedCalendarEvents();
                  }

                  @Override
                  protected int getResultResource() {
                    return R.string.EPr_manage_delete_completed_gcal_status;
                  }
                }.execute())
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void deleteAllCalendarEvents() {
    dialogBuilder
        .newMessageDialog(R.string.EPr_manage_delete_all_gcal_message)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) ->
                new ProgressDialogAsyncTask(OldTaskPreferences.this, dialogBuilder) {
                  @Override
                  protected Integer doInBackground(Void... params) {
                    calendarEventProvider.deleteEvents(taskDao.getAllCalendarEvents());
                    return taskDao.clearAllCalendarEvents();
                  }

                  @Override
                  protected int getResultResource() {
                    return R.string.EPr_manage_delete_all_gcal_status;
                  }
                }.execute())
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void resetPreferences() {
    dialogBuilder
        .newMessageDialog(R.string.EPr_reset_preferences_warning)
        .setPositiveButton(
            R.string.EPr_reset_preferences,
            (dialog, which) -> {
              preferences.reset();
              System.exit(0);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void deleteTaskData() {
    dialogBuilder
        .newMessageDialog(R.string.EPr_delete_task_data_warning)
        .setPositiveButton(
            R.string.EPr_delete_task_data,
            (dialog, which) -> {
              deleteDatabase(database.getName());
              System.exit(0);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
