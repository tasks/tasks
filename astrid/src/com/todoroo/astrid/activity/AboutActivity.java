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

import java.util.Formatter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;

import com.timsu.astrid.R;

/**
 * Displays an About ("End User License Agreement") that the user has to accept
 * before using the application. Your application should call
 * {@link About#showAbout(android.app.Activity)} in the onCreate() method of the
 * first activity. If the user accepts the About, it will never be shown again.
 * If the user refuses, {@link android.app.Activity#finish()} is invoked on your
 * activity.
 */
class About {
    private static final String PREFERENCES_ABOUT = "About"; //$NON-NLS-1$

    /**
     * Displays the About if necessary. This method should be called from the
     * onCreate() method of your main Activity.
     *
     * @param activity
     *            The Activity to finish if the user rejects the About
     */
    static void showAbout(final Activity activity, final Resources r, final String versionName) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.p_about);
        builder.setCancelable(true);
        builder.setMessage((new Formatter()).format(r.getString(R.string.p_about_text), versionName).toString());
        builder.show();
    }

    private About() {
        // don't construct me
    }
}