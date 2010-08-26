package com.todoroo.astrid.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.webkit.WebView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.dao.Database;


public final class UpgradeService {

    private static final int V3_2_6 = 153;
    private static final int V3_2_5 = 152;
    private static final int V3_2_4 = 151;
    private static final int V3_2_3 = 150;
    private static final int V3_1_0 = 146;
    private static final int V3_0_6 = 145;
    private static final int V3_0_5 = 144;
    private static final int V3_0_0 = 136;
    private static final int V2_14_4 = 135;

    @Autowired
    private Database database;

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

        // pop up a progress dialog
        final ProgressDialog dialog;
        if(context instanceof Activity)
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
                                if(dialog != null)
                                    dialog.dismiss();

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

        if(from <= V2_14_4)
            newVersionString(changeLog, "3.2.0 (8/16/10)", new String[] {
                    "Astrid is brand new inside and out! In addition to a new " +
                    "look and feel, a new add-on system allows Astrid to become " +
                    "more powerful, while other improvements have made it faster " +
                    "and easier to use. Hope you like it!",
                    "This update contains for free all of Astrid " +
                    "Power Pack's features for evaluation purposes",
                    "If you liked the old version, you can also go back by " +
                    "<a href='http://bit.ly/oldastrid'>clicking here</a>",
            });
        if(from > V3_1_0 && from <= V3_2_6)
            newVersionString(changeLog, "3.2.7 (8/25/10)", new String[] {
                    "Fixed: crazy notifications for overdue tasks! :(",
            });
        if(from > V3_1_0 && from <= V3_2_5)
            newVersionString(changeLog, "3.2.7 (8/24/10)", new String[] {
                    "RTM: fix for login popping up randomly, not syncing priority",
                    "Sync: added a 'Sync Now!' button to the menu, moved options to Settings",
                    "Improvements to notification code to remind you of missed notifications",
                    "Smoother scrolling of task list once details are loaded",
            });
        if(from > V3_1_0 && from <= V3_2_4)
            newVersionString(changeLog, "3.2.5 (8/18/10)", new String[] {
                    "Fix for duplicated tasks created in RTM",
            });
        if(from > V3_1_0 && from <= V3_2_3)
            newVersionString(changeLog, "3.2.5 (8/18/10)", new String[] {
                    "Fix for duplicated tasks created in Producteev",
                    "Fix for being able to create tasks without title",
            });
        if(from > V2_14_4 && from <= V3_1_0)
            newVersionString(changeLog, "3.2.0 (8/16/10)", new String[] {
                    "Build your own custom filters from the Filter page",
                    "Easy task sorting (in the task list menu)",
                    "Create widgets from any of your filters",
                    "Synchronize with Producteev! (producteev.com)",
                    "Select tags by drop-down box",
                    "Cosmetic improvements, calendar & sync bug fixes",
            });
        if(from > V2_14_4 && from <= V3_0_6)
            newVersionString(changeLog, "3.1.0 (8/9/10)", new String[] {
                    "Linkify phone numbers, e-mails, and web pages",
                    "Swipe L => R to go from tasks to filters",
                    "Moved task priority bar to left side",
                    "Added ability to create fixed alerts for a task",
                    "Restored tag hiding when tag begins with underscore (_)",
                    "FROYO: disabled moving app to SD card, it would break alarms and widget",
                    "Also gone: a couple force closes, bugs with repeating tasks",
            });
        if(from > V2_14_4 && from <= V3_0_5)
            newVersionString(changeLog, "3.0.6 (8/4/10)", new String[] {
                    "This update contains for free all of the " +
                        "powerpack's features for evaluation purposes",
                    "Fixed widget not updating when tasks are edited",
                    "Added a setting for displaying task notes in the list",
            });

        if(changeLog.length() == 0)
            return;

        changeLog.append("Enjoy!</body></html>");
        String changeLogHtml = "<html><body style='color: white'>" + changeLog;

        WebView webView = new WebView(context);
        webView.loadData(changeLogHtml, "text/html", "utf-8");
        webView.setBackgroundColor(0);

        new AlertDialog.Builder(context)
        .setTitle(R.string.UpS_changelog_title)
        .setView(webView)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(android.R.string.ok, null)
        .show();
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
