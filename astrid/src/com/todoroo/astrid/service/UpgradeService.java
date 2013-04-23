/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

import com.timsu.astrid.GCMIntentService;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.AstridNewSyncMigrator;
import com.todoroo.astrid.actfm.sync.EmptyTitleOutstandingEntryMigration;
import com.todoroo.astrid.actfm.sync.messages.ConvertSelfUserIdsToZero;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.Eula;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.core.SavedFilter;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.helper.DueDateTimeMigrator;
import com.todoroo.astrid.service.abtesting.ABChooser;
import com.todoroo.astrid.subtasks.SubtasksMetadataMigration;
import com.todoroo.astrid.tags.TagCaseMigrator;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;


public final class UpgradeService {

    public static final int V4_6_5 = 306;
    public static final int V4_6_4 = 305;
    public static final int V4_6_3 = 304;
    public static final int V4_6_2 = 303;
    public static final int V4_6_1 = 302;
    public static final int V4_6_0 = 301;
    public static final int V4_6_0_BETA = 300;
    public static final int V4_5_3 = 294;
    public static final int V4_5_2 = 293;
    public static final int V4_5_1 = 292;
    public static final int V4_5_0 = 291;
    public static final int V4_4_4_1 = 290;
    public static final int V4_4_4 = 289;
    public static final int V4_4_3 = 288;
    public static final int V4_4_2 = 287;
    public static final int V4_4_1 = 286;
    public static final int V4_4 = 285;
    public static final int V4_3_4_2 = 284;
    public static final int V4_3_4_1 = 283;
    public static final int V4_3_4 = 282;
    public static final int V4_3_3 = 281;
    public static final int V4_3_2 = 280;
    public static final int V4_3_1 = 279;
    public static final int V4_3_0 = 278;
    public static final int V4_2_6 = 277;
    public static final int V4_2_5 = 276;
    public static final int V4_2_4 = 275;
    public static final int V4_2_3 = 274;
    public static final int V4_2_2_1 = 273;
    public static final int V4_2_2 = 272;
    public static final int V4_2_1 = 271;
    public static final int V4_2_0 = 270;
    public static final int V4_1_3_1 = 269;
    public static final int V4_1_3 = 268;
    public static final int V4_1_2 = 267;
    public static final int V4_1_1 = 266;
    public static final int V4_1_0 = 265;
    public static final int V4_0_6_2 = 264;
    public static final int V4_0_6_1 = 263;
    public static final int V4_0_6 = 262;
    public static final int V4_0_5_1 = 261;
    public static final int V4_0_5 = 260;
    public static final int V4_0_4_3 = 259;
    public static final int V4_0_4_2 = 258;
    public static final int V4_0_4_1 = 257;
    public static final int V4_0_4 = 256;
    public static final int V4_0_3 = 255;
    public static final int V4_0_2_1 = 254;
    public static final int V4_0_2 = 253;
    public static final int V4_0_1 = 252;
    public static final int V4_0_0 = 251;
    public static final int V3_9_2_3 = 210;
    public static final int V3_9_2_2 = 209;
    public static final int V3_9_2_1 = 208;
    public static final int V3_9_2 = 207;
    public static final int V3_9_1_1 = 206;
    public static final int V3_9_1 = 205;
    public static final int V3_9_0_2 = 204;
    public static final int V3_9_0_1 = 203;
    public static final int V3_9_0 = 202;
    public static final int V3_8_5_1 = 201;
    public static final int V3_8_5 = 200;
    public static final int V3_8_4_4 = 199;
    public static final int V3_8_4_3 = 198;
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

    @Autowired AddOnService addOnService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

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
    public void performUpgrade(final Activity context, final int from) {
        if(from == 135)
            AddOnService.recordOem();

        if(from > 0 && from < V3_8_2) {
            if(Preferences.getBoolean(R.string.p_transparent_deprecated, false))
                Preferences.setString(R.string.p_theme, "transparent"); //$NON-NLS-1$
            else
                Preferences.setString(R.string.p_theme, "black"); //$NON-NLS-1$
        }

        if(from <= V3_9_1_1) {
            actFmPreferenceService.clearLastSyncDate();
        }

        final String lastSetVersionName = AstridPreferences.getCurrentVersionName();

        Preferences.setInt(AstridPreferences.P_UPGRADE_FROM, from);

        int maxWithUpgrade = V4_6_3; // The last version that required a migration

        Preferences.setInt(AstridPreferences.P_UPGRADE_FROM, from);

        inlineUpgrades(context, from); // Migrations that are short or don't require launching a separate activity

        if(from < maxWithUpgrade) {
            Intent upgrade = new Intent(context, UpgradeActivity.class);
            upgrade.putExtra(UpgradeActivity.TOKEN_FROM_VERSION, from);
            context.startActivityForResult(upgrade, 0);
        }
    }

    public static class UpgradeActivity extends Activity {
        @Autowired
        private TaskService taskService;
        private ProgressDialog dialog;

        public static final String TOKEN_FROM_VERSION = "from_version"; //$NON-NLS-1$
        private int from;
        private boolean finished = false;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            DependencyInjectionService.getInstance().inject(this);
            from = getIntent().getIntExtra(TOKEN_FROM_VERSION, -1);
            if (from > 0) {
                dialog = DialogUtilities.progressDialog(this,
                        getString(R.string.DLG_upgrading));
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            if(from < V3_0_0)
                                new Astrid2To3UpgradeHelper().upgrade2To3(UpgradeActivity.this, from);

                            if(from < V3_1_0)
                                new Astrid2To3UpgradeHelper().upgrade3To3_1(UpgradeActivity.this, from);

                            if(from < V3_8_3_1)
                                new TagCaseMigrator().performTagCaseMigration(UpgradeActivity.this);

                            if(from < V3_8_4 && Preferences.getBoolean(R.string.p_showNotes, false))
                                taskService.clearDetails(Task.NOTES.neq("")); //$NON-NLS-1$

                            if (from < V4_0_6)
                                new DueDateTimeMigrator().migrateDueTimes();

                            if (from < V4_4_2)
                                new SubtasksMetadataMigration().performMigration();

                            if (from < V4_6_0_BETA) {
                                if (Preferences.getBoolean(R.string.p_use_filters, false)) {
                                    TodorooCursor<StoreObject> cursor = PluginServices.getStoreObjectDao().query(Query.select(StoreObject.PROPERTIES).where(
                                            StoreObject.TYPE.eq(SavedFilter.TYPE)).limit(1));
                                    try {
                                        if (cursor.getCount() == 0)
                                            Preferences.setBoolean(R.string.p_use_filters, false);
                                    } finally {
                                        cursor.close();
                                    }
                                }
                                new AstridNewSyncMigrator().performMigration();
                                new GCMIntentService.GCMMigration().performMigration(UpgradeActivity.this);
                            }

                            if (from < V4_6_2) {
                                new EmptyTitleOutstandingEntryMigration().performMigration();
                            }

                            if (from < V4_6_3) {
                                new ConvertSelfUserIdsToZero().execute();
                            }

                        } finally {
                            finished = true;
                            DialogUtilities.dismissDialog(UpgradeActivity.this, dialog);
                            sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                            setResult(AstridActivity.RESULT_RESTART_ACTIVITY);
                            finish();
                        }
                    };
                }.start();
            } else {
                finished = true;
                finish();
            }
        }

        @Override
        public void onBackPressed() {
            // Don't allow the back button to finish this activity before things are done
            if (finished)
                super.onBackPressed();
        }
    }

    private void inlineUpgrades(Context context, int from) {
        if (from < V4_5_1) {
            String key = context.getString(R.string.p_taskRowStyle);
            if (Preferences.isSet(key)) {
                boolean value = Preferences.getBoolean(key, false);
                Preferences.clear(key);
                Preferences.setString(R.string.p_taskRowStyle_v2, value ? "1" : "0"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        if (from < V4_6_2) {
            Preferences.setBoolean(R.string.p_show_timer_shortcut, true);
        }

        if (from < V4_6_3) {
            if (ActFmPreferenceService.isPremiumUser())
                Preferences.clear(NameMaps.PUSHED_AT_TAGS);
            Preferences.setLong(TaskListFragment.PREF_LAST_FEEDBACK_TIME, DateUtilities.now());
        }
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

        Preferences.clear(TagCaseMigrator.PREF_SHOW_MIGRATION_ALERT);

        Preferences.clear(AstridPreferences.P_UPGRADE_FROM);
        StringBuilder changeLog = new StringBuilder();

        if (from >= V4_6_0 && from < V4_6_4) { // Versions 4.6.4 and 4.6.5 are virtually identical--the former went out to the Amazon store only
            newVersionString(changeLog, "4.6.5 (4/23/13)", new String[] {
                 "Improvements to monthly repeating tasks scheduled for the end of the month",
                 "Minor bugfixes"
            });
        }

        if (from < V4_6_3) {
            newVersionString(changeLog, "4.6.3 (4/15/13)", new String[] {
                    "BIG RELEASE!",
                    "\"Waiting on me\" shows shared tasks assigned to you or where you were mentioned by name",
                    "Sync fixes for non-english languages and for multiple android devices",
                    "Scrollable widgets and other Power Pack features now free! Thank you to everyone who purchased one-time add-ons and supported Astrid through the years!"
            });
        }

        if (from >= V4_6_0 && from < V4_6_2) {
            newVersionString(changeLog, "4.6.2 (4/10/13)", new String[] {
                "Fixed a bug that could prevent certain tasks from syncing correctly",
                "UI improvements"
            });
        }

        if (from >= V4_6_0 && from < V4_6_1) {
            newVersionString(changeLog, "4.6.1 (4/04/13)", new String[] {
                "Improved design for the task edit page",
                "Fixed issues with Facebook login",
                "Several crash fixes"
            });
        }

        if (from < V4_6_0) {
            newVersionString(changeLog, "4.6.0 (4/02/13)", new String[] {
                    "4.6 is a big step towards FLAWLESS sync with Astrid.com! This update also includes numerous bug fixes and UI improvements. " +
                    "Note: Simultaneous sync with Google Tasks and Astrid.com is no longer supported. Still want to view your tasks in Gmail? Try Remind Me by Astrid on the Chrome Store."
            });
        }

        if (from >= V4_5_0 && from < V4_5_3) {
            newVersionString(changeLog, "4.5.3 (2/04/13)", new String[] {
                "Performance improvements when scrolling in lists",
                "Some crash fixes"
            });
        }

        if (from >= V4_5_0 && from < V4_5_2) {
            newVersionString(changeLog, "4.5.2 (1/17/13)", new String[] {
                "Fixed a crash that could affect some Google Tasks users",
                "UI polish"
            });
        }

        if (from >= V4_5_0 && from < V4_5_1) {
            newVersionString(changeLog, "4.5.1 (1/16/13)", new String[] {
                "New 'Titles only' style option for task lists (Settings > Appearance > Task row appearance)",
                "Bug and crash fixes"
            });
        }

        if (from < V4_5_0) {
            newVersionString(changeLog, "4.5.0 (12/19/12)", new String[] {
                "Several interface and usability enhancements",
                "Fixed Power Pack display bug affecting certain phones",
                "Better organized preferences",
                "New 'Sky Blue' theme",
                "'Lite mode' preference defaults available in Settings > Appearance > Set configuration",
                "Minor bug and crash fixes"
            });
        }

        if (from >= V4_4_4 && from < V4_4_4_1) {
            newVersionString(changeLog, "4.4.4.1 (12/13/12)", new String[] {
                "Crash fixes"
            });
        }

        if (from >= V4_4 && from < V4_4_4) {
            newVersionString(changeLog, "4.4.4 (12/12/12)", new String[] {
                "Manual ordering and subtasks for 'Active Tasks' and 'Today' filters now sync with Astrid.com",
                "Minor polish and bug fixes"
            });
        }

        if (from >= V4_4 && from < V4_4_3) {
            newVersionString(changeLog, "4.4.3 (11/28/12)", new String[] {
                "Minor bug fixes"
            });
        }

        if (from < V4_4_2) {
            newVersionString(changeLog, "4.4.2 (11/19/12)", new String[] {
                "Manual order and subtasks for lists now sync with your Astrid.com account!",
                "Significant performance improvements to manual ordering and subtasks",
                "Show assigned user images in the premium widgets",
                "Minor bug and crash fixes"
            });
        }

        if (from >= V4_4 && from < V4_4_1) {
            newVersionString(changeLog, "4.4.1 (10/30/12)", new String[] {
                "Fixed an issue where the calendar assistant could remind you about the wrong events",
                "Fixed a crash that could occur when swipe between lists is enabled"
            });
        }

        if (from < V4_4) {
            newVersionString(changeLog, "4.4 (10/25/12)", new String[] {
                "Astrid calendar assistant will help you prepare for meetings! Enable or " +
                    "disable in Settings -> Premium and misc. settings -> Calendar assistant",
                "Full Chinese translation",
                "Widgets will now reflect manual ordering",
                "Accept pending friend requests from the people view",
                "Several minor bug and crash fixes"
            });
        }

        if (from >= V4_3_0 && from < V4_3_4) {
            newVersionString(changeLog, "4.3.4 (10/2/12)", new String[] {
                "Magic words in task title now correctly removed when placed in parentheses",
                "Bug fix for viewing Featured Lists with swipe enabled",
                "Bug fixes for rare crashes"
            });
        }

        if (from >= V4_3_0 && from < V4_3_3) {
            newVersionString(changeLog, "4.3.3 (9/19/12)", new String[] {
               "Reverse sort works again!",
               "Swipe between lists is faster",
               "Option for a new style of task row (Try it: Settings -> Appearance -> Task row appearance and choose \"Simple\")",
               "A few other small bugs fixed"
            });
        }

        if (from >= V4_3_0 && from < V4_3_2) {
            newVersionString(changeLog, "4.3.2 (9/07/12)", new String[] {
               "Fixed issues with Google Tasks shortcuts"
            });
        }

        if (from >= V4_3_0 && from < V4_3_1) {
            newVersionString(changeLog, "4.3.1 (9/06/12)", new String[] {
               "Fixed crashes for Google Tasks users and certain filters",
               "Added ability to complete shared tasks from list view"
            });
        }

        if (from < V4_3_0) {
            newVersionString(changeLog, "4.3.0 (9/05/12)", new String[] {
               "Significant performance improvements",
               "Voice-add now enabled for all users!",
               "New feature: Featured lists (Enable in Settings -> Appearance)",
               "New and improved premium widget design",
               "Redesigned settings page",
               "Improved translations",
               "Lots of bug fixes and UI polish"
            });
        }

        if (from >= V4_2_0 && from < V4_2_6) {
            newVersionString(changeLog, "4.2.6 (8/14/12)", new String[] {
               "Tablet users can opt to use the single-pane phone layout (Settings -> Astrid Labs)",
               "Minor polish and bug fixes"
            });
        }

        if (from >= V4_2_0 && from < V4_2_5) {
            newVersionString(changeLog, "4.2.5 (8/13/12)", new String[] {
               "Improved tablet UX",
               "More customizable task edit screen",
               "Add to calendar now starts at task due time (by default)",
               "Updated translations for several languages",
               "Numerous bug fixes"
            });
        }

        if (from >= V4_2_0 && from < V4_2_4) {
            newVersionString(changeLog, "4.2.4 (7/11/12)", new String[] {
               "Ability to specify end date on repeating tasks",
               "Improved sync of comments made while offline",
               "Crash fixes"
            });
        }

        if (from >= V4_2_0 && from < V4_2_3) {
            newVersionString(changeLog, "4.2.3 (6/25/12)", new String[] {
               "Fixes for Google Tasks and Astrid.com sync",
               "New layout for tablets in portrait mode",
               "Minor UI polish and bugfixes"
            });
        }

        if (from >= V4_2_0 && from < V4_2_2_1) {
            newVersionString(changeLog, "4.2.2.1 (6/13/12)", new String[] {
               "Fixed a crash affecting the Nook"
            });
        }

        if (from >= V4_2_0 && from < V4_2_2) {
            newVersionString(changeLog, "4.2.2 (6/12/12)", new String[] {
               "Fix for people views displaying the wrong list of tasks",
               "Fixed a bug with Astrid Smart Sort",
               "Fixed a bug when adding photo comments"
            });
        }

        if (from >= V4_2_0 && from < V4_2_1) {
            newVersionString(changeLog, "4.2.1 (6/08/12)", new String[] {
                "Fix for MyTouch 4G Lists",
                "Fixed a crash when adding tasks with due times to Google Calendar",
                "Better syncing of the people list",
                "Minor UI polish and bugfixes"
            });
        }

        if (from < V4_2_0) {
            newVersionString(changeLog, "4.2.0 (6/05/12)", new String[] {
                "Support for the Nook",
                "Fixed crash on large image attachments",
                "Minor bugfixes"
            });
        }

        if (from >= V4_1_3 && from < V4_1_3_1) {
            newVersionString(changeLog, "4.1.3.1 (5/18/12)", new String[] {
                "Fixed reminders for ICS"
            });
        }

        if (from >= V4_1_2 && from < V4_1_3) {
            newVersionString(changeLog, "4.1.3 (5/17/12)", new String[] {
                "Added ability to see shared tasks sorted by friend! Enable or disable " +
                "in Settings > Astrid Labs",
                "Fixed desktop shortcuts",
                "Fixed adding tasks from the Power Pack widget",
                "Fixed selecting photos on the Kindle Fire",
                "Various other minor bug and crash fixes"
            });
        }

        if (from >= V4_1_1 && from < V4_1_2) {
            newVersionString(changeLog, "4.1.2 (5/05/12)", new String[] {
                "Fixed some crashes and minor bugs"
            });
        }

        if (from < V4_1_1) {
            newVersionString(changeLog, "4.1.1 (5/04/12)", new String[] {
                    "Respond to or set reminders for missed calls. This feature requires a new permission to read " +
                    "the phone state.",
            });
        }

        if (from < V4_1_0) {
            newVersionString(changeLog, "4.1.0 (5/03/12)", new String[] {
                "Swipe between lists! Swipe left and right to move through your lists. Enable or adjust " +
                "in Settings > Astrid Labs",
                "Assign tasks to contacts without typing",
                "Links to tasks in comments",
                "Astrid.com sync improvements",
                "Other minor bugfixes",
            });
        }

        if (from >= V4_0_6 && from < V4_0_6_2) {
            newVersionString(changeLog, "4.0.6.2 (4/03/12)", new String[] {
                "Minor fix to backup migration fix to handle deleted tasks as well as completed tasks."
            });
        }

        if (from >= V4_0_6 && from < V4_0_6_1) {
            newVersionString(changeLog, "4.0.6.1 (4/03/12)", new String[] {
                    "Fixed a bug where old tasks could become uncompleted. Sorry to those of you" +
                    " who were affected by this! To recover, you can import your old tasks" +
                    " from any backup file created before April 3 by clicking Menu -> Settings ->" +
                    " Backups -> Manage Backups -> Import Tasks. Backup files from April 3 will start" +
                    " with 'auto.120403'."
            });
        }

        if (from < V4_0_6) {
            newVersionString(changeLog, "4.0.6 (4/02/12)", new String[] {
                    "Fixes and performance improvements to Astrid.com and Google Tasks sync",
                    "Google TV support! (Beta)",
                    "Fixed a bug that could put duetimes on tasks when changing timezones",
                    "Fixed a rare crash when starting a task timer"
            });
        }

        if (from >= V4_0_0 && from < V4_0_5) {
            newVersionString(changeLog, "4.0.5 (3/22/12)", new String[] {
               "Better conflict resolution for Astrid.com sync",
               "Fixes and improvements to Gtasks sync",
               "Added option to report sync errors in sync preference screen"
            });
        }

        if (from >= V4_0_0 && from < V4_0_4) {
            newVersionString(changeLog, "4.0.4 (3/7/12)", new String[] {
               "Fixed crashes related to error reporting",
               "Fixed a crash when creating a task from the widget",
               "Fixed a bug where a manual sync wouldn't always start"
            });
        }

        if (from >= V4_0_0 && from < V4_0_3) {
            newVersionString(changeLog, "4.0.3 (3/6/12)", new String[] {
               "Fix some issues with Google Tasks sync. We're sorry to " +
                   "everyone who's been having trouble with it!",
               "Updated translations for Portuguese, Chinese, German, Russian, and Dutch",
               "Centralize Android's menu key with in-app navigation",
               "Fixed crashes & improve crash logging",
            });
        }

        if (from >= V4_0_0 && from < V4_0_2) {
            newVersionString(changeLog, "4.0.2 (2/29/12)", new String[] {
                    "Removed GPS permission - no longer needed",
                    "Fixes for some subtasks issues",
                    "No longer need to run the Crittercism service in the background",
                    "Fixed a crash that could occur when cloning tasks",
                    "Fixed a bug that prevented certain comments from syncing correctly",
                    "Fixed issues where voice add wouldn't work correctly",
            });
        }

        if (from >= V4_0_0 && from < V4_0_1) {
            newVersionString(changeLog, "4.0.1 (2/23/12)", new String[] {
               "Fixed a database issue affecting Android 2.1 users",
               "Fixed a crash when using drag and drop in Google Tasks lists",
               "Other small bugfixes"
            });
        }

        if (from < V4_0_0) {
            newVersionString(changeLog, "4.0.0 (2/23/12)", new String[] {
               "Welcome to Astrid 4.0! Here's what's new:",
               "<b>Subtasks!!!</b><br>Press the Menu key and select 'Sort' to access",
               "<b>New Look!</b><br>Customize how Astrid looks from the Settings menu",
               "<b>Task Rabbit!</b><br>Outsource your tasks with the help of trustworthy people",
               "<b>More Reliable Sync</b><br>Including fixes to Astrid.com and Google Tasks sync",
               "<b>Tablet version</b><br>Enjoy Astrid on your luxurious Android tablet",
               "Many bug and usability fixes"
            });
        }

        // --- old messages

        if (from >= V3_0_0 && from < V3_9_0) {
            newVersionString(changeLog, "3.9 (12/09/11)", new String[] {
                    "Cleaner design (especially the task edit page)!",
                    "Customize the edit page (\"Beast Mode\" in preferences)",
                    "Make shared lists with tasks open to anyone (perfect for potlucks, road trips etc)",
                    "Fixes for some ICS crashes (full support coming soon)",
                    "Google Tasks sync improvement - Note: If you have been experiencing \"Sync with errors\", try logging out and logging back in to Google Tasks.",
                    "Other minor bug fixes",
                    "Feedback welcomed!"
            });
        }

        if(from >= V3_0_0 && from < V3_8_0) {
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

        changeLog.append("Enjoy!</body></html>");
        String color = ThemeService.getDialogTextColorString();
        String changeLogHtml = "<html><body style='color: " + color +"'>" + changeLog;

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
        if (Constants.ASTRID_LITE)
            version = "0" + version.substring(1);
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
