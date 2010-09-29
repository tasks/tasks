package com.todoroo.astrid.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.webkit.WebView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.dao.Database;


public final class UpgradeService {

    public static final int V3_3_0 = 155;
    public static final int V3_2_0 = 147;
    public static final int V3_1_0 = 146;
    public static final int V3_0_0 = 136;
    public static final int V2_14_4 = 135;

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
            newVersionString(changeLog, "3.3.0 (9/12/10)", new String[] {
                    "Astrid is brand new inside and out! In addition to a new " +
                    "look and feel, a new add-on system allows Astrid to become " +
                    "more powerful, while other improvements have made it faster " +
                    "and easier to use. Hope you like it!",
                    "If you liked the old version, you can also go back by " +
                    "<a href='http://bit.ly/oldastrid'>clicking here</a>",
            });
        if(from >= V3_3_0)
            newVersionString(changeLog, "3.3.6 (9/27/10)", new String[] {
                    "Restored alarm functionality",
                    "Producteev: sync can now remove due dates in Producteev",
            });
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

    // --- upgrade functions

    @SuppressWarnings({"nls", "unused"})
    private void upgrade3To3_4(final Context context) {
        // if RTM, store RTM to secondary preferences
        if(Preferences.getStringValue("rmilk_token") != null) {
            SharedPreferences settings = context.getSharedPreferences("rtm", Context.MODE_WORLD_READABLE);
            Editor editor = settings.edit();
            editor.putString("rmilk_token", Preferences.getStringValue("rmilk_token"));
            editor.putLong("rmilk_last_sync", Preferences.getLong("rmilk_last_sync", 0));
            editor.commit();

            final String message = "Hi, it looks like you are a Remember the Milk user! " +
            		"In this version of Astrid, RTM is now a community-supported " +
            		"add-on. Please go to the Android market to install it!";
            if(context instanceof Activity) {
                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(context)
                        .setTitle(com.todoroo.astrid.api.R.string.DLG_information_title)
                        .setMessage(message)
                        .setPositiveButton("Go To Market", new AddOnService.MarketClickListener(context, "org.weloveastrid.rmilk"))
                        .setNegativeButton("Later", null)
                        .show();
                    }
                });
            }
        }
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
