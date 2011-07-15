package com.todoroo.astrid.service;

import java.util.Date;

import org.weloveastrid.rmilk.data.MilkNoteHelper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.Eula;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.notes.NoteMetadata;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;
import com.todoroo.astrid.utility.AstridPreferences;


public final class UpgradeService {

    public static final int V3_8_0 = 186;
    public static final int V3_7_7 = 184;
    public static final int V3_7_6 = 182;
    public static final int V3_7_5 = 179;
    public static final int V3_7_4 = 178;
    public static final int V3_7_3 = 175;
    public static final int V3_7_2 = 174;
    public static final int V3_7_1 = 173;
    public static final int V3_7_0 = 172;
    public static final int V3_6_4 = 170;
    public static final int V3_6_3 = 169;
    public static final int V3_6_2 = 168;
    public static final int V3_6_0 = 166;
    public static final int V3_5_0 = 165;
    public static final int V3_4_0 = 162;
    public static final int V3_3_0 = 155;
    public static final int V3_2_0 = 147;
    public static final int V3_1_0 = 146;
    public static final int V3_0_0 = 136;
    public static final int V2_14_4 = 135;

    @Autowired Database database;

    @Autowired TaskService taskService;

    @Autowired MetadataService metadataService;

    @Autowired GtasksPreferenceService gtasksPreferenceService;

    public UpgradeService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Perform upgrade from one version to the next. Needs to be called
     * on the UI thread so it can display a progress bar and then
     * show users a change log.
     *
     * @param from
     * @param to
     */
    public void performUpgrade(final Context context, final int from) {
        if(from == 135)
            AddOnService.recordOem();

        // long running tasks: pop up a progress dialog
        final ProgressDialog dialog;
        if(from < V3_0_0 && context instanceof Activity)
            dialog = DialogUtilities.progressDialog(context,
                    context.getString(R.string.DLG_upgrading));
        else
            dialog = null;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(from < V3_0_0)
                        new Astrid2To3UpgradeHelper().upgrade2To3(context, from);

                    if(from < V3_1_0)
                        new Astrid2To3UpgradeHelper().upgrade3To3_1(context, from);

                } finally {
                    if(context instanceof Activity) {
                        ((Activity)context).runOnUiThread(new Runnable() {
                            public void run() {
                                DialogUtilities.dismissDialog((Activity)context, dialog);

                                // display changelog
                                showChangeLog(context, from);
                                if(context instanceof TaskListActivity)
                                    ((TaskListActivity)context).loadTaskListContent(true);
                            }
                        });
                    }
                }
            }

        }).start();
    }

    /**
     * Return a change log string. Releases occur often enough that we don't
     * expect change sets to be localized.
     *
     * @param from
     * @param to
     * @return
     */
    @SuppressWarnings("nls")
    public void showChangeLog(Context context, int from) {
        if(!(context instanceof Activity) || from == 0)
            return;

        StringBuilder changeLog = new StringBuilder();

        if(from <= V2_14_4) {
            newVersionString(changeLog, "3.7.2 (3/04/11)", new String[] {
                    "Astrid is brand new inside and out! In addition to a new " +
                    "look and feel, a new add-on system allows Astrid to become " +
                    "more powerful, while other improvements have made it faster " +
                    "and easier to use. Hope you like it!",
                    "If you liked the old version, you can also go back by " +
                    "<a href='http://bit.ly/oldastrid'>clicking here</a>",
            });
        } else {
            // current message
            if(from < V3_8_0) {
                newVersionString(changeLog, "3.8.0 (7/15/11)", new String[] {
                        "Astrid.com: sync & share tasks / lists with others!",
                        "GTasks Sync using Google's official task API! Gtasks users" +
                            "will need to perform a manual sync to set everything up.",
                        "New style for \"Task Edit\" page!",
                        "Purge completed or deleted tasks!",
                });

                // clear gtasks token info because of new gtasks api
                gtasksPreferenceService.setToken(null);
            }
            if(from < V3_7_7) {
                newVersionString(changeLog, "3.7.7 (6/22/11, bug fixes, usability, cosmetic)", new String[] {
                        "Moved note viewing into quick action bar based on usability feedback",
                        "Fix for repeats not working with weekly intervals over 1 week",
                        "Fix for Producteev notes being cleared when new notes arrive",
                        "Fix for default reminder settings not being applied to Google/Producteev tasks",
                });
            }
            if(from >= V3_7_0 && from < V3_7_6) {
                newVersionString(changeLog, "3.7.6 (6/13/11)", new String[] {
                        "Fix for Astrid overwriting Google / Producteev values during sync",
                        "No more snoozes for old repeating task iterations",
                        "Fix for some alarms not being cancelled when due date changes",
                });
            }
            if(from >= V3_7_0 && from < V3_7_5) {
                newVersionString(changeLog, "3.7.5 (5/24/11)", new String[] {
                        "Fix for custom filter not working with tags with apostrophes",
                        "Fix for crashes when rearranging tasks in a Google list",
                        "Fix for yellow line drawn when swiping",
                        "Fix for various crashes",
                });
            }
            if(from >= V3_7_0 && from < V3_7_4) {
                newVersionString(changeLog, "3.7.4 (5/19/11)", new String[] {
                        "Fix for Galaxy S preference issue duplicating intro tasks",
                        "Fix for Producteev note order incorrect",
                        "Fix for repeating tasks causing duplicates during sync",
                        "Fix for quick-add tasks in a Producteev workspace not working",
                        "Fix for repeat from completion date losing due time",
                        "Fix for viewing a task causng local changes to overwrite during sync",
                        "Integration with OpenCRX (opencrx.org)",
                });
                Preferences.setBoolean(Eula.PREFERENCE_EULA_ACCEPTED, true);
            }
            if(from >= V3_7_0 && from < V3_7_3) {
                newVersionString(changeLog, "3.7.3 (4/26/11)", new String[] {
                        "Fixed 'Show Notes' setting not working",
                        "Added setting for transparent background in Astrid",
                        "Google Tasks list no longer shows completed tasks",
                        "Added Google Tasks lists to custom filter",
                        "Fixed bugs and increased speed in Google Tasks sync",
                });
                if(gtasksPreferenceService.isLoggedIn())
                    taskService.clearDetails(Criterion.all);
            }
            if(from >= V3_7_0 && from < V3_7_2) {
                newVersionString(changeLog, "3.7.2 (3/04/11)", new String[] {
                        "Added a 'Ring 5 times' setting to task reminders",
                        "Added 'Copy Task' to the long-press menu",
                        "Fixed crash for Motorola Xoom / Android Honeycomb users",
                        "Fixed some Producteev-related errors and duplicated task issues.",
                        "Fixed issue with importing repeating tasks from XML",
                });
            }
            if(from >= V3_7_0 && from < V3_7_1) {
                newVersionString(changeLog, "3.7.1 (2/17/11)", new String[] {
                    "Fixed due date reminders not being displayed",
                    "Added an 'About' screen for displaying current version.",
                    "Fixed several user-reported crashes! Keep 'em coming :)",
                });
            }
            if(from >= V3_0_0 && from < V3_7_0) {
                newVersionString(changeLog, "3.7.0 (2/7/11)", new String[] {
                        "Improved UI for displaying task actions. Tap a task to " +
                            "bring up actions, tap again to dismiss.",
                        "Task notes can be viewed by tapping the note icon to " +
                            "the right of the task.",
                        "Added Astrid as 'Send-To' choice in Android Browser and " +
                            "other apps.",
                        "Add tags and importance in quick-add, e.g. " +
                            "\"call mom #family @phone !4\"",
                        "Fixed bug with custom filters & tasks being hidden.",
                });
                upgrade3To3_7();
            }

            // old messages
            if(from >= V3_6_0 && from < V3_6_4) {
                newVersionString(changeLog, "3.6.4 (12/28/10)", new String[] {
                        "Fix crash occuring when using timers",
                });
            }
            if(from >= V3_6_0 && from < V3_6_3) {
                newVersionString(changeLog, "3.6.3 (12/18/10)", new String[] {
                        "Added support for Producteev repeating tasks",
                        "Fix for Producteev sync duplicate task issues",
                });
            }
            if(from >= V3_6_0 && from < V3_6_2) {
                newVersionString(changeLog, "3.6.2 (12/11/10)", new String[] {
                        "Fix for Google Tasks crash on view list",
                        "Fix for Producteev crash sometimes during sync",
                });
            }
            if(from >= V3_0_0 && from < V3_6_0) {
                newVersionString(changeLog, "3.6.0 (11/13/10)", new String[] {
                        "Astrid Power Pack is now launched to the Android Market. " +
                        "New Power Pack features include 4x2 and 4x4 widgets and voice " +
                        "task reminders and creation. Go to the add-ons page to find out more!",
                        "Fix for Google Tasks: due times got lost on sync, repeating tasks not repeated",
                        "Fix for task alarms not always firing if multiple set",
                        "Fix for various force closes",
                });
                upgrade3To3_6(context);
            }
            if(from >= V3_0_0 && from < V3_5_0)
                newVersionString(changeLog, "3.5.0 (10/25/10)", new String[] {
                        "Google Tasks Sync (beta!)",
                        "Bug fix with RMilk & new tasks not getting synced",
                        "Fixed Force Closes and other bugs",
                });
            if(from >= V3_0_0 && from < V3_4_0) {
                newVersionString(changeLog, "3.4.0 (10/08/10)", new String[] {
                        "End User License Agreement",
                        "Option to disable usage statistics",
                        "Bug fixes with Producteev",
                });
            }
            if(from >= V3_0_0 && from < V3_3_0)
                newVersionString(changeLog, "3.3.0 (9/17/10)", new String[] {
                        "Fixed some RTM duplicated tasks issues",
                        "UI updates based on your feedback",
                        "Snooze now overrides other alarms",
                        "Added preference option for selecting snooze style",
                        "Hide until: now allows you to pick a specific time",
                });
            if(from >= V3_0_0 && from < V3_2_0)
                newVersionString(changeLog, "3.2.0 (8/16/10)", new String[] {
                        "Build your own custom filters from the Filter page",
                        "Easy task sorting (in the task list menu)",
                        "Create widgets from any of your filters",
                        "Synchronize with Producteev! (producteev.com)",
                        "Select tags by drop-down box",
                        "Cosmetic improvements, calendar & sync bug fixes",
                });
            if(from >= V3_0_0 && from < V3_1_0)
                newVersionString(changeLog, "3.1.0 (8/9/10)", new String[] {
                        "Linkify phone numbers, e-mails, and web pages",
                        "Swipe L => R to go from tasks to filters",
                        "Moved task priority bar to left side",
                        "Added ability to create fixed alerts for a task",
                        "Restored tag hiding when tag begins with underscore (_)",
                        "FROYO: disabled moving app to SD card, it would break alarms and widget",
                        "Also gone: a couple force closes, bugs with repeating tasks",
                });
        }

        if(changeLog.length() == 0)
            return;

        changeLog.append("Astrid thinks you are very special!</body></html>");
        String changeLogHtml = "<html><body style='color: white'>" + changeLog;

        DialogUtilities.htmlDialog(context, changeLogHtml,
                R.string.UpS_changelog_title);
    }

    /**
     * Helper for adding a single version to the changelog
     * @param changeLog
     * @param version
     * @param changes
     */
    @SuppressWarnings("nls")
    private void newVersionString(StringBuilder changeLog, String version, String[] changes) {
        changeLog.append("<font style='text-align: center; color=#ffaa00'><b>Version ").append(version).append(":</b></font><br><ul>");
        for(String change : changes)
            changeLog.append("<li>").append(change).append("</li>\n");
        changeLog.append("</ul>");
    }

    // --- upgrade functions

    /**
     * Fixes task filter missing tasks bug, migrate PDV/RTM notes
     */
    @SuppressWarnings("nls")
    private void upgrade3To3_7() {
        TodorooCursor<Task> t = taskService.query(Query.select(Task.ID, Task.DUE_DATE).where(Task.DUE_DATE.gt(0)));
        Task task = new Task();
        for(t.moveToFirst(); !t.isAfterLast(); t.moveToNext()) {
            task.readFromCursor(t);
            if(task.hasDueDate()) {
                task.setValue(Task.DUE_DATE, task.getValue(Task.DUE_DATE) / 1000L * 1000L);
                taskService.save(task);
            }
        }
        t.close();

        TodorooCursor<Metadata> m = metadataService.query(Query.select(Metadata.PROPERTIES).
                where(Criterion.or(Metadata.KEY.eq("producteev-note"),
                        Metadata.KEY.eq("rmilk-note"))));

        StringProperty PDV_NOTE_ID = Metadata.VALUE1;
        StringProperty PDV_NOTE_MESSAGE = Metadata.VALUE2;
        LongProperty PDV_NOTE_CREATED = new LongProperty(Metadata.TABLE, Metadata.VALUE3.name);

        StringProperty RTM_NOTE_ID = Metadata.VALUE1;
        StringProperty RTM_NOTE_TITLE = Metadata.VALUE2;
        StringProperty RTM_NOTE_TEXT = Metadata.VALUE3;
        LongProperty RTM_NOTE_CREATED = new LongProperty(Metadata.TABLE, Metadata.VALUE4.name);

        Metadata metadata = new Metadata();
        for(m.moveToFirst(); !m.isAfterLast(); m.moveToNext()) {
            metadata.readFromCursor(m);

            String id, body, title, provider;
            long created;
            if("rmilk-note".equals(metadata.getValue(Metadata.KEY))) {
                id = metadata.getValue(RTM_NOTE_ID);
                body = metadata.getValue(RTM_NOTE_TEXT);
                title = metadata.getValue(RTM_NOTE_TITLE);
                created = metadata.getValue(RTM_NOTE_CREATED);
                provider = MilkNoteHelper.PROVIDER;
            } else {
                id = metadata.getValue(PDV_NOTE_ID);
                body = metadata.getValue(PDV_NOTE_MESSAGE);
                created = metadata.getValue(PDV_NOTE_CREATED);
                title = DateUtilities.getDateStringWithWeekday(ContextManager.getContext(),
                    new Date(created));
                provider = ProducteevDataService.NOTE_PROVIDER;
            }

            metadata.setValue(Metadata.KEY, NoteMetadata.METADATA_KEY);
            metadata.setValue(Metadata.CREATION_DATE, created);
            metadata.setValue(NoteMetadata.BODY, body);
            metadata.setValue(NoteMetadata.TITLE, title);
            metadata.setValue(NoteMetadata.THUMBNAIL, null);
            metadata.setValue(NoteMetadata.EXT_PROVIDER, provider);
            metadata.setValue(NoteMetadata.EXT_ID, id);

            metadata.clearValue(Metadata.ID);
            metadataService.save(metadata);
        }
        m.close();
    }

    /**
     * Moves sorting prefs to public pref store
     * @param context
     */
    private void upgrade3To3_6(final Context context) {
        SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(context);
        Editor editor = publicPrefs.edit();
        editor.putInt(SortHelper.PREF_SORT_FLAGS,
                Preferences.getInt(SortHelper.PREF_SORT_FLAGS, 0));
        editor.putInt(SortHelper.PREF_SORT_SORT,
                Preferences.getInt(SortHelper.PREF_SORT_SORT, 0));
        editor.commit();
    }

    // --- secondary upgrade

    /**
     * If primary upgrade doesn't work for some reason (corrupt SharedPreferences,
     * for example), this will catch some cases
     */
    public void performSecondaryUpgrade(Context context) {
        if(!context.getDatabasePath(database.getName()).exists() &&
                context.getDatabasePath("tasks").exists()) { //$NON-NLS-1$
            new Astrid2To3UpgradeHelper().upgrade2To3(context, 1);
        }
    }


}
