/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.Preferences;
import org.tasks.ui.ProgressDialogAsyncTask;

import javax.inject.Inject;

public class OldTaskPreferences extends InjectingPreferenceActivity {

    @Inject StartupService startupService;
    @Inject DialogBuilder dialogBuilder;
    @Inject TaskService taskService;
    @Inject GCalHelper gcalHelper;
    @Inject TaskDeleter taskDeleter;
    @Inject MetadataDao metadataDao;
    @Inject Preferences preferences;
    @Inject Database database;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startupService.onStartupApplication(this);

        addPreferencesFromResource(R.xml.preferences_oldtasks);

        findPreference(getString(R.string.EPr_manage_purge_deleted)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                purgeDeletedTasks();
                return false;
            }
        });

        findPreference(getString(R.string.EPr_manage_delete_completed)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                deleteCompletedTasks();
                return false;
            }
        });

        findPreference(getString(R.string.EPr_manage_delete_completed_gcal)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                deleteCompletedEvents();
                return false;
            }
        });

        findPreference(getString(R.string.EPr_manage_delete_all_gcal)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                deleteAllCalendarEvents();
                return false;
            }
        });

        findPreference(getString(R.string.EPr_reset_preferences)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                resetPreferences();
                return false;
            }
        });

        findPreference(getString(R.string.EPr_delete_task_data)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                deleteTaskData();
                return false;
            }
        });
    }

    private void deleteCompletedTasks() {
        dialogBuilder.newMessageDialog(R.string.EPr_manage_delete_completed_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new ProgressDialogAsyncTask(OldTaskPreferences.this, dialogBuilder) {
                            @Override
                            protected Integer doInBackground(Void... params) {
                                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.CALENDAR_URI).where(
                                        Criterion.and(Task.COMPLETION_DATE.gt(0), Task.CALENDAR_URI.isNotNull())));
                                try {
                                    int length = cursor.getCount();
                                    for (int i = 0; i < length; i++) {
                                        cursor.moveToNext();
                                        Task task = new Task(cursor);
                                        gcalHelper.deleteTaskEvent(task);
                                    }
                                } finally {
                                    cursor.close();
                                }
                                Task template = new Task();
                                template.setDeletionDate(DateUtilities.now());
                                return taskService.update(Task.COMPLETION_DATE.gt(0), template);
                            }

                            @Override
                            protected int getResultResource() {
                                return R.string.EPr_manage_delete_completed_status;
                            }
                        }.execute();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void purgeDeletedTasks() {
        dialogBuilder.newMessageDialog(R.string.EPr_manage_purge_deleted_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new ProgressDialogAsyncTask(OldTaskPreferences.this, dialogBuilder) {
                            @Override
                            protected Integer doInBackground(Void... params) {
                                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.TITLE, Task.CALENDAR_URI).where(
                                        Criterion.and(Task.DELETION_DATE.gt(0), Task.CALENDAR_URI.isNotNull())));
                                try {
                                    int length = cursor.getCount();
                                    for (int i = 0; i < length; i++) {
                                        cursor.moveToNext();
                                        Task task = new Task(cursor);
                                        gcalHelper.deleteTaskEvent(task);
                                    }
                                } finally {
                                    cursor.close();
                                }
                                int result = taskDeleter.purgeDeletedTasks();
                                metadataDao.removeDanglingMetadata();
                                return result;
                            }

                            @Override
                            protected int getResultResource() {
                                return R.string.EPr_manage_purge_deleted_status;
                            }
                        }.execute();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteCompletedEvents() {
        dialogBuilder.newMessageDialog(R.string.EPr_manage_delete_completed_gcal_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new ProgressDialogAsyncTask(OldTaskPreferences.this, dialogBuilder) {

                            @Override
                            protected Integer doInBackground(Void... params) {
                                int deletedEventCount = 0;
                                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.CALENDAR_URI).where(
                                        Criterion.and(Task.COMPLETION_DATE.gt(0), Task.CALENDAR_URI.isNotNull())));
                                try {
                                    int length = cursor.getCount();
                                    for (int i = 0; i < length; i++) {
                                        cursor.moveToNext();
                                        Task task = new Task(cursor);
                                        if (gcalHelper.deleteTaskEvent(task)) {
                                            deletedEventCount++;
                                        }
                                    }
                                } finally {
                                    cursor.close();
                                }
                                // mass update the CALENDAR_URI here,
                                // since the GCalHelper doesnt save it due to performance-reasons
                                Task template = new Task();
                                template.setCalendarUri(""); //$NON-NLS-1$
                                taskService.update(
                                        Criterion.and(Task.COMPLETION_DATE.gt(0), Task.CALENDAR_URI.isNotNull()),
                                        template);
                                return deletedEventCount;
                            }

                            @Override
                            protected int getResultResource() {
                                return R.string.EPr_manage_delete_completed_gcal_status;
                            }
                        }.execute();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteAllCalendarEvents() {
        dialogBuilder.newMessageDialog(R.string.EPr_manage_delete_all_gcal_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new ProgressDialogAsyncTask(OldTaskPreferences.this, dialogBuilder) {
                            @Override
                            protected Integer doInBackground(Void... params) {
                                int deletedEventCount = 0;
                                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.CALENDAR_URI).where(
                                        Task.CALENDAR_URI.isNotNull()));
                                try {
                                    int length = cursor.getCount();
                                    for (int i = 0; i < length; i++) {
                                        cursor.moveToNext();
                                        Task task = new Task(cursor);
                                        if (gcalHelper.deleteTaskEvent(task)) {
                                            deletedEventCount++;
                                        }
                                    }
                                } finally {
                                    cursor.close();
                                }
                                // mass update the CALENDAR_URI here,
                                // since the GCalHelper doesnt save it due to performance-reasons
                                Task template = new Task();
                                template.setCalendarUri(""); //$NON-NLS-1$
                                taskService.update(Task.CALENDAR_URI.isNotNull(), template);
                                return deletedEventCount;
                            }

                            @Override
                            protected int getResultResource() {
                                return R.string.EPr_manage_delete_all_gcal_status;
                            }
                        }.execute();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void resetPreferences() {
        dialogBuilder.newMessageDialog(R.string.EPr_reset_preferences_warning)
                .setPositiveButton(R.string.EPr_reset_preferences, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.reset();
                        System.exit(0);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteTaskData() {
        dialogBuilder.newMessageDialog(R.string.EPr_delete_task_data_warning)
                .setPositiveButton(R.string.EPr_delete_task_data, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteDatabase(database.getName());
                        System.exit(0);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }
}
