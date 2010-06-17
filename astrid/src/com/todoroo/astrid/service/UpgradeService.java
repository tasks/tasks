package com.todoroo.astrid.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.webkit.WebView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;


public final class UpgradeService {

    /**
     * Perform upgrade from one version to the next. Needs to be called
     * on the UI thread so it can display a progress bar and then
     * show users a change log.
     *
     * @param from
     * @param to
     */
    public void performUpgrade(int from) {
        if(from < 1)
            return;

        if(from < 135)
            new Astrid2To3UpgradeHelper().upgrade2To3();

        // display changelog
        showChangeLog(from);
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
    public void showChangeLog(int from) {
        if(!(ContextManager.getContext() instanceof Activity))
            return;

        StringBuilder changeLog = new StringBuilder();

        if(from <= 130)
            newVersionString(changeLog, "2.14.0 (5/24/10)", new String[] {
                    "Pick a calendar to 'Add to Calendar' (in Settings menu)",
                    "RTM: archived lists are ignored",
                    "Fixed user-reported crashes!"});
        if(from > 130 && from <= 131)
            newVersionString(changeLog, "2.14.1 (5/29/10)", new String[] {
                    "Fixed crash while using PureCalendar widget",
            });
        if(from > 130 && from <= 132)
            newVersionString(changeLog, "2.14.2 (5/29/10)", new String[] {
                    "Fixed crashes occuring with certain languages (Spanish, Polish)",
                    "Fixed backup service deleting too many old days backups",
            });
        if(from > 130 && from <= 133)
            newVersionString(changeLog, "2.14.3 (6/11/10)", new String[] {
                    "Fixed crashes occuring with certain languages (Swedish, Turkish)",
                    "Fixed other crashes that users have reported",
            });
        if(from <= 134)
            newVersionString(changeLog, "3.0.0 (?/??/10)", new String[] {
                    "Astrid is brand new under the hood! You won't see many " +
                        "changes yet but Astrid received a much-needed makeover " +
                        "that allows it to do a lot of new tricks. Stay tuned!",
            });

        if(changeLog.length() == 0)
            return;

        changeLog.append("</body></html>");
        String changeLogHtml = "<html><body style='color: white'>" + changeLog;

        WebView webView = new WebView(ContextManager.getContext());
        webView.loadData(changeLogHtml, "text/html", "utf-8");
        webView.setBackgroundColor(0);

        new AlertDialog.Builder(ContextManager.getContext())
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
