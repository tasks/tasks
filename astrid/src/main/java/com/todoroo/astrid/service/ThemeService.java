/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;

import org.tasks.R;

public class ThemeService {

    @Deprecated
    public static int getEditDialogTheme() {
        boolean ics = AndroidUtilities.getSdkVersion() >= 14;
        int themeSetting = Preferences.getBoolean(R.string.p_use_dark_theme, false) ? R.style.Tasks : R.style.Tasks_Light;
        int theme;
        if (themeSetting == R.style.Tasks) {
            if (ics) {
                theme = R.style.TEA_Dialog_ICS;
            } else {
                theme = R.style.TEA_Dialog;
            }
        } else {
            if (ics) {
                theme = R.style.TEA_Dialog_Light_ICS;
            } else {
                theme = R.style.TEA_Dialog_Light;
            }
        }
        return theme;
    }
}
