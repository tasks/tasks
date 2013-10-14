/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.utility.AstridPreferences;

import org.tasks.R;

public class ThemeService {

    public static final String THEME_WHITE = "white";
    public static final String THEME_BLACK = "black";

    public static final int FLAG_FORCE_DARK = 1;
    public static final int FLAG_FORCE_LIGHT = 2;
    public static final int FLAG_INVERT = 3;

    // Widget config activities set this flag since they theme differently than the normal
    // filter list. In other cases this should be false
    private static boolean forceFilterInvert = false;

    public static void applyTheme(Activity activity) {
        int currentTheme = getTheme();
        activity.setTheme(currentTheme);
        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    }

    public static int getTheme() {
        String preference = Preferences.getBoolean(R.string.p_use_dark_theme, false) ? THEME_BLACK : THEME_WHITE;
        return getStyleForSetting(preference);
    }

    public static int getWidgetTheme() {
        String preference = Preferences.getBoolean(R.string.p_use_dark_theme_widget, false) ? THEME_BLACK : THEME_WHITE;
        if (TextUtils.isEmpty(preference)) {
            return getTheme();
        } else {
            return getStyleForSetting(preference);
        }
    }

    private static int getStyleForSetting(String setting) {
        if(THEME_BLACK.equals(setting)) {
            return R.style.Theme;
        } else {
            return R.style.Theme_White;
        }
    }

    public static int getThemeColor() {
        int theme = getTheme();
        switch(theme) {
        case R.style.Theme:
            return R.color.blue_theme_color;
        case R.style.Theme_White:
        default:
            return R.color.dark_blue_theme_color;
        }
    }

    public static int getEditDialogTheme() {
        boolean ics = AndroidUtilities.getSdkVersion() >= 14;
        int themeSetting = getTheme();
        int theme;
        if (themeSetting == R.style.Theme) {
            if (ics) {
                theme = R.style.TEA_Dialog_ICS;
            } else {
                theme = R.style.TEA_Dialog;
            }
        } else {
            if (ics) {
                theme = R.style.TEA_Dialog_White_ICS;
            } else {
                theme = R.style.TEA_Dialog_White;
            }
        }
        return theme;
    }

    public static int getDialogTheme() {
        int themeSetting = getTheme();
        int theme;
        if (themeSetting == R.style.Theme) {
            theme = R.style.Theme_Dialog;
        } else {
            theme = R.style.Theme_Dialog_White;
        }
        return theme;
    }

    public static int getDialogTextColor() {
        if (AndroidUtilities.getSdkVersion() >= 11) {
            int theme = getTheme();
            if (theme == R.style.Theme) {
                return android.R.color.white;
            } else {
                return android.R.color.black;
            }
        } else {
            return android.R.color.white;
        }
    }

    public static String getDialogTextColorString() {
        int color = getDialogTextColor();
        if (color == android.R.color.white) {
            return "white";
        }
        return "black";
    }

    public static int getDrawable(int lightDrawable) {
        return getDrawable(lightDrawable, 0);
    }

    /**
     * Only widget config activities should call this (see note on the flag above)
     */
    public static void setForceFilterInvert(boolean forceInvert) {
        forceFilterInvert = forceInvert;
    }

    public static int getFilterThemeFlags() {
        if (forceFilterInvert) {
            return ThemeService.FLAG_INVERT;
        }
        if (AstridPreferences.useTabletLayout(ContextManager.getContext())) {
            return ThemeService.FLAG_FORCE_LIGHT;
        }
        return 0;
    }

    public static int getDrawable(int lightDrawable, int alter) {
        int theme = getTheme();
        boolean darkTheme = theme == R.style.Theme;
        switch(alter) {
        case FLAG_FORCE_DARK:
            darkTheme = true;
            break;
        case FLAG_FORCE_LIGHT:
            darkTheme = false;
            break;
        case FLAG_INVERT:
            darkTheme = !darkTheme;
            break;
        default:
            break;
        }

        if (lightDrawable == R.drawable.icn_menu_refresh &&
                AstridPreferences.useTabletLayout(ContextManager.getContext())) {
            return R.drawable.icn_menu_refresh_tablet;
        }

        if(!darkTheme) {
            return lightDrawable;
        }

        switch(lightDrawable) {
        case R.drawable.ic_action_mic:
            return R.drawable.ic_action_mic_light;
        case R.drawable.ic_action_save:
            return R.drawable.ic_action_save_light;
        case R.drawable.ic_action_discard:
            return R.drawable.ic_action_discard_light;
        case R.drawable.ic_action_cancel:
            return R.drawable.ic_action_cancel_light;
        case R.drawable.ic_action_new_attachment:
            return R.drawable.ic_action_new_attachment_light;
        case R.drawable.ic_action_gear:
            return R.drawable.ic_action_gear_light;
        case R.drawable.icn_menu_refresh:
            return R.drawable.icn_menu_refresh_dark;
        case R.drawable.icn_menu_filters:
            return R.drawable.icn_menu_filters_dark;
        case R.drawable.icn_menu_sort_by_size:
            return R.drawable.icn_menu_sort_by_size_dark;
        case R.drawable.icn_menu_search:
            return R.drawable.icn_menu_search_dark;
        case R.drawable.icn_menu_friends:
            return R.drawable.icn_menu_friends_dark;
        case R.drawable.icn_menu_lists:
            return R.drawable.icn_menu_lists_dark;
        case R.drawable.icn_menu_settings:
            return R.drawable.icn_menu_settings_dark;
        case R.drawable.filter_assigned:
            return R.drawable.filter_assigned_dark;
        case R.drawable.filter_calendar:
            return R.drawable.filter_calendar_dark;
        case R.drawable.filter_inbox:
            return R.drawable.filter_inbox_dark;
        case R.drawable.filter_pencil:
            return R.drawable.filter_pencil_dark;
        case R.drawable.filter_sliders:
            return R.drawable.filter_sliders_dark;
        case R.drawable.gl_lists:
            return R.drawable.gl_lists_dark;
        }

        Log.w("ThemeService", "No theme drawable found for " + ContextManager.getResources().getResourceName(lightDrawable));
        return lightDrawable;
    }

    public static int getDarkVsLight(int resForWhite, int resForDark) {
        int theme = getTheme();
        if (theme == R.style.Theme) {
            return resForDark;
        } else {
            return resForWhite;
        }
    }
    public static int getTaskEditDrawable(int regularDrawable, int lightBlueDrawable) {
        return getDarkVsLight(regularDrawable, lightBlueDrawable);
    }

    public static int getTaskEditThemeColor() {
        return getDarkVsLight(R.color.task_edit_selected, R.color.blue_theme_color);
    }
}
