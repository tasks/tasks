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
import android.content.res.Resources;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
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
    @SuppressWarnings("nls")
    public static void showAbout(final Activity activity, final String versionName) {

        Resources r = activity.getResources();

        StringBuilder aboutText = new StringBuilder();
        aboutText.append("<b>").append(r.getString(R.string.app_name)).append("</b><br />").
            append(r.getString(R.string.p_about_text, versionName).replace("\n", "<br />")).append("<br /><br />").
            append("<a href='http://github.com/todoroo/astrid'>Source Code</a><br />").
    		append("<a href='http://astrid.com/privacy'>Privacy Policy</a><br />").
    		append("<a href='http://astrid.com/terms'>Terms of Use</a><br /><br />").
    		append("Visit <a href='http://astrid.com'>astrid.com</a> " +
    				"for more information, to add translations or help make Astrid better!");

        final AlertDialog.Builder d = new AlertDialog.Builder(activity);

        Spanned body = Html.fromHtml(aboutText.toString());
        TextView textView = new TextView(activity);
        textView.setText(body);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setPadding(5, 0, 5, 0);

        d.setIcon(android.R.drawable.ic_dialog_info);
        d.setView(textView);
        d.setTitle(r.getString(R.string.p_about));
        d.show();
    }

    private About() {
        // don't construct me
    }
}
