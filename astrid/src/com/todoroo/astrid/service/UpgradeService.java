package com.todoroo.astrid.service;

import java.util.ArrayList;
import java.util.Collections;
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
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.notes.NoteMetadata;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;
import com.todoroo.astrid.service.abtesting.ABChooser;
import com.todoroo.astrid.service.abtesting.ABOptions;
import com.todoroo.astrid.tags.TagCaseMigrator;
import com.todoroo.astrid.utility.AstridPreferences;


public final class UpgradeService {

    public static final int V3_8_4_2 = 197;
    public static final int V3_8_4_1 = 196;
    public static final int V3_8_4 = 195;
    public static final int V3_8_3_1 = 194;
    public static final int V3_8_3 = 192;
    public static final int V3_8_2 = 191;
    public static final int V3_8_0_2 = 188;
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

    @Autowired ABChooser abChooser;

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

        if(from > 0 && from < V3_8_2) {
            if(Preferences.getBoolean(R.string.p_transparent_deprecated, false))
                Preferences.setString(R.string.p_theme, "transparent"); //$NON-NLS-1$
            else
                Preferences.setString(R.string.p_theme, "black"); //$NON-NLS-1$
        }

        // long running tasks: pop up a progress dialog
        final ProgressDialog dialog;
        if(from < V3_0_0 && context instanceof Activity)
            dialog = DialogUtilities.progressDialog(context,
                    context.getString(R.string.DLG_upgrading));
        else
            dialog = null;

        Preferences.setInt(AstridPreferences.P_UPGRADE_FROM, from);

        if (from <= V3_8_3_1) // This needs to happen synchronously
            abChooser.setChoiceForOption(ABOptions.AB_OPTION_FIRST_ACTIVITY, 0);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(from < V3_0_0)
                        new Astrid2To3UpgradeHelper().upgrade2To3(context, from);

                    if(from < V3_1_0)
                        new Astrid2To3UpgradeHelper().upgrade3To3_1(context, from);

                    if(from < V3_8_3_1)
                        new TagCaseMigrator().performTagCaseMigration(context);

                    if(from < V3_8_4 && Preferences.getBoolean(R.string.p_showNotes, true))
                        taskService.clearDetails(Task.NOTES.neq("")); //$NON-NLS-1$
                } finally {
                    DialogUtilities.dismissDialog((Activity)context, dialog);
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

        boolean showTagCaseMigration = Preferences.getBoolean(TagCaseMigrator.PREF_SHOW_MIGRATION_ALERT, false);
        Preferences.clear(TagCaseMigrator.PREF_SHOW_MIGRATION_ALERT);

        Preferences.clear(AstridPreferences.P_UPGRADE_FROM);
        StringBuilder changeLog = new StringBuilder();

        // current message
        if (from >= V3_8_4 && from < V3_8_4_2) {
            newVersionString(changeLog, "3.8.4.2 (10/03/11)", new String[] {
                    "Updated Google Tasks API libraries to 1.2.2"
            });
        }
        if(from >= V3_8_4 && from < V3_8_4_1) {
            newVersionString(changeLog, "3.8.4.1 (9/30/11)", new String[] {
                    "Fixed a but where completing weekly repeat-after-complete tasks set due time",
            });
        }
        if (from < V3_8_4) {
            String[] base = new String[] {
                    "Lists are now case insensitive. If you have existing lists with the same " +
                        "name, except for capitalization, they will be renamed to avoid collision.",
                    "Fixed google tasks login on non-US Samsung phones",
                    "Astrid.com sync now occurring over HTTPS",
                    "Repeat from completion date now synced to Astrid.com",
                    "Added ability to post a picture in list comments with Astrid.com",
                    "Reorganized list view when synchronizing with Astrid.com to highlight shared/private lists",
                    "Added default notification type, fixed default calendar in task default settings",
                    "Fixed bug where wrong task would get checked when completing a task",
                    "Fixed widgets created from lists not launching the app",
                    "Repeating tasks don't lose reminders anymore",
                    "Empty lists should now show up in the dropdown menu on task edit page",
            };
            ArrayList<String> stringList = new ArrayList<String>();
            Collections.addAll(stringList, base);
            if (showTagCaseMigration)
                stringList.add(0, context.getString(R.string.tag_case_migration_notice));

            newVersionString(changeLog, "3.8.4 (9/30/11)", stringList.toArray(new String[stringList.size()]));
        }
        if(from >= V3_8_0 && from < V3_8_3) {
            newVersionString(changeLog, "3.8.3 (9/02/11)", new String[] {
                    "Several Google Tasks sync bug fixes / improvements",
                    "Fixed voice-add while viewing lists",
                    "Fix for Astrid.com keyboard covers up newest comments",
                    "Fixed issues with repeat-after-complete hourly and minutely",
                    "Google Calendar integration now deletes events when you delete tasks",
                    "Sync preference page now shows who you are logged in as",
                    "List task counts are working again",
                    "Fixed reminders for tasks without due time notifying repeatedly",
                    "Fixed day-of-week checkboxes not having labels",
                    "Fixed completing repeating tasks from powerpack widget broken",
                    "Added 'default add to calendar' setting for new task creation",
            });
        }
        if(from >= V3_8_0 && from < V3_8_2) {
            newVersionString(changeLog, "3.8.2 (8/10/11)", new String[] {
                    "New theme options for Astrid - white, black, transparent with white or black text",
                    "New style for filter list page - same great functionality",
                    "Search for tasks by pressing the search hardware button",
                    "Fix for failed Google Tasks migration sync",
                    "Fix for Google Tasks sync date-off-by-one error",
                    "Fix for Astrid.com deleting tag information when syncing",
                    "Fix for Astrid.com and repeat-after-complete",
                    "Fix for some Astrid.com timeout and task sync errors",
                    "Fix for duplicated Producteev notes",
                    "Fix for reported market crashes",
                    "Updated translations to latest",
                    "And, Astrid has trimmed down by 25 percent in memory size!",
            });
        }
        if(from >= V3_8_0 && from < V3_8_0_2) {
            newVersionString(changeLog, "3.8.0.2 (7/16/11)", new String[] {
                    "Fix for due time lost during Astrid.com sync",
                    "Fix for disappearing Producteev workspace editor",
                    "Fix for crashes. Keep on reporting them!",
            });
        }
        if(from < V3_8_0) {
            newVersionString(changeLog, "3.8.0 (7/15/11)", new String[] {
                    "Astrid.com: sync & share tasks / lists with others!",
                    "GTasks Sync using Google's official task API! Gtasks users " +
                        "will need to perform a manual sync to set everything up.",
                    "Renamed \"Tags\" to \"Lists\" (see blog.astrid.com for details)",
                    "New style for \"Task Edit\" page!",
                    "Purge completed or deleted tasks from settings menu!",
            });
            gtasksPreferenceService.setToken(null);
        }

        // --- old messages

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
            if(gtasksPreferenceService.isLoggedIn())
                taskService.clearDetails(Criterion.all);
            Preferences.setBoolean(Eula.PREFERENCE_EULA_ACCEPTED, true);
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

        if(changeLog.length() == 0)
            return;

        changeLog.append("Go get 'em, tiger!</body></html>");
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
