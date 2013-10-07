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
import android.os.Bundle;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.utility.AstridPreferences;

import org.tasks.R;

public final class UpgradeService {

    public static final int V4_6_5 = 306;
    public static final int V3_0_0 = 136;

    @Autowired Database database;

    @Autowired TaskService taskService;

    @Autowired MetadataService metadataService;

    @Autowired GtasksPreferenceService gtasksPreferenceService;

    @Autowired AddOnService addOnService;

    public UpgradeService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Perform upgrade from one version to the next. Needs to be called
     * on the UI thread so it can display a progress bar and then
     * show users a change log.
     */
    public void performUpgrade(final Activity context, final int from) {
        Preferences.setInt(AstridPreferences.P_UPGRADE_FROM, from);

        int maxWithUpgrade = V4_6_5;

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
            if (finished) {
                super.onBackPressed();
            }
        }
    }

    /**
     * Return a change log string. Releases occur often enough that we don't
     * expect change sets to be localized.
     */
    public void showChangeLog(Context context, int from) {
        if(!(context instanceof Activity) || from == 0) {
            return;
        }

        Preferences.clear(AstridPreferences.P_UPGRADE_FROM);
        StringBuilder changeLog = new StringBuilder();

        if (from >= 0 && from < V4_6_5) {
            newVersionString(changeLog, "4.6.5 (4/23/13)", new String[] {
                 "Improvements to monthly repeating tasks scheduled for the end of the month",
                 "Minor bugfixes"
            });
        }

        if(changeLog.length() == 0) {
            return;
        }

        changeLog.append("Enjoy!</body></html>");
        String color = ThemeService.getDialogTextColorString();
        String changeLogHtml = "<html><body style='color: " + color +"'>" + changeLog;

        DialogUtilities.htmlDialog(context, changeLogHtml,
                R.string.UpS_changelog_title);
    }

    /**
     * Helper for adding a single version to the changelog
     */
    private void newVersionString(StringBuilder changeLog, String version, String[] changes) {
        changeLog.append("<font style='text-align: center; color=#ffaa00'><b>Version ").append(version).append(":</b></font><br><ul>");
        for(String change : changes) {
            changeLog.append("<li>").append(change).append("</li>\n");
        }
        changeLog.append("</ul>");
    }
}
