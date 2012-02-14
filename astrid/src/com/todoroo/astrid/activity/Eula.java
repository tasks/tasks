/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;

/**
 * Displays an EULA ("End User License Agreement") that the user has to accept
 * before using the application. Your application should call
 * {@link Eula#showEula(android.app.Activity)} in the onCreate() method of the
 * first activity. If the user accepts the EULA, it will never be shown again.
 * If the user refuses, {@link android.app.Activity#finish()} is invoked on your
 * activity.
 */
public final class Eula {
    public static final String PREFERENCE_EULA_ACCEPTED = "eula.accepted"; //$NON-NLS-1$

    @Autowired TaskService taskService;

    /**
     * Displays the EULA if necessary. This method should be called from the
     * onCreate() method of your main Activity.
     *
     * @param activity
     *            The Activity to finish if the user rejects the EULA
     */
    public static void showEula(final Activity activity) {
        if(!new Eula().shouldShowEula(activity))
            return;

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.DLG_eula_title);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.DLG_accept,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        accept(activity);
                    }
                });
        builder.setNegativeButton(R.string.DLG_decline,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        refuse(activity);
                    }
                });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                refuse(activity);
            }
        });
        builder.setMessage(AndroidUtilities.readFile(activity, R.raw.eula));
        builder.show();
    }

    public static void showEulaBasic(Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.DLG_eula_title);
        builder.setMessage(AndroidUtilities.readFile(activity, R.raw.eula));
        builder.setNeutralButton(android.R.string.ok, null);
        builder.show();
    }

    private boolean shouldShowEula(Activity activity) {
        if(Preferences.getBoolean(PREFERENCE_EULA_ACCEPTED, false))
            return false;

        SharedPreferences p = activity.getSharedPreferences("eula", Activity.MODE_PRIVATE); //$NON-NLS-1$
        if(p.getBoolean(PREFERENCE_EULA_ACCEPTED, false))
            return false;

        if(taskService.countTasks() > 0)
            return false;
        return true;
    }

    private static void accept(Activity activity) {
        if (activity instanceof EulaCallback) {
            ((EulaCallback)activity).eulaAccepted();
        }
        Preferences.setBoolean(PREFERENCE_EULA_ACCEPTED, true);
        StatisticsService.reportEvent(StatisticsConstants.EULA_ACCEPTED);
    }

    private static void refuse(Activity activity) {
        if (activity instanceof EulaCallback) {
            ((EulaCallback)activity).eulaRefused();
        }
        activity.finish();
    }

    public static interface EulaCallback {
        public void eulaAccepted();
        public void eulaRefused();
    }

    private Eula() {
        // don't construct me
        DependencyInjectionService.getInstance().inject(this);
    }
}
