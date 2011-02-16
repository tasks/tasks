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
import android.text.util.Linkify;
import android.widget.TextView;

import com.timsu.astrid.R;

/**
 * Displays an About dialog.
 */
class About {
    /**
     * Displays the About dialog from the settings menu.
     *
     * @param activity For context.
     */
    static void showAbout(final Activity activity, final Resources r, final String versionName) {
        final AlertDialog.Builder d = new AlertDialog.Builder(activity);
        final TextView t = new TextView(activity);
        t.setText((new Formatter()).format(r.getString(R.string.p_about_text), versionName).toString());
        Linkify.addLinks(t, Linkify.ALL);
        d.setView(t);
        d.show();
    }

    private About() {
        // don't construct me
    }
}