/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MultilineHelper {
    protected static void makeMultiline(View view) {
        if (view instanceof ViewGroup) {

            ViewGroup grp = (ViewGroup) view;

            for (int index = 0; index < grp.getChildCount(); index++) {
                makeMultiline(grp.getChildAt(index));
            }
        } else if (view instanceof TextView) {
            TextView t = (TextView) view;
            t.setSingleLine(false);
            t.setEllipsize(null);
        }
    }
}
