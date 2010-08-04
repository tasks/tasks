package com.todoroo.astrid.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.webkit.WebView;

import com.timsu.astrid.R;


public final class UpgradeService {

    /**
     * Perform upgrade from one version to the next. Needs to be called
     * on the UI thread so it can display a progress bar and then
     * show users a change log.
     *
     * @param from
     * @param to
     */
    public void performUpgrade(Context context, int from) {
        if(from == 135)
            AddOnService.recordOem();

        if(from < 136) {
            new Astrid2To3UpgradeHelper().upgrade2To3(context, this, from);
        } else {
            // display changelog
            showChangeLog(context, from);
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

        StringBuilder changeLog = new StringBuilder();

        if(from <= 135)
            newVersionString(changeLog, "3.0.0 (8/3/10)", new String[] {
                    "Astrid is brand new inside and out! In addition to a new " +
                    "look and feel, a new add-on system allows Astrid to become " +
                    "more powerful, while other improvements have made it faster " +
                    "and easier to use. Hope you like it!",
            });
        else if(from > 135 && from <= 140)
            newVersionString(changeLog, "3.0.2 (8/4/10)", new String[] {
                    "Upgrade note: if you are missing old repeating tasks, " +
                    "search for them, there was a bug where they were marked " +
                    "as completed.",
                    "This update also fixes widget issues with Android 1.5. ",
                    "Thanks for your patience!",
            });
        else if(from > 140 && from <= 142)
            newVersionString(changeLog, "3.0.4 (8/4/10)", new String[] {
                    "Making upgrade easier for new Astrid 3 users.",
                    "Fixed some user-reported crashes",
                    "We love you!!",
            });

        if(changeLog.length() == 0)
            return;

        changeLog.append("</body></html>");
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

    // --- database upgrade logic

}
