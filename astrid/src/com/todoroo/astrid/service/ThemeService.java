/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.text.TextUtils;
import android.view.WindowManager;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.widget.TasksWidget;

@SuppressWarnings("nls")
public class ThemeService {

    public static final String THEME_WHITE = "white";
    public static final String THEME_WHITE_RED = "white-red";
    public static final String THEME_BLACK = "black";
    public static final String THEME_TRANSPARENT = "transparent";
    public static final String THEME_TRANSPARENT_WHITE = "transparent-white";

    public static final String THEME_WIDGET_SAME_AS_APP = "same-as-app";
    public static final String THEME_WIDGET_LEGACY = "legacy-widget";

    public static final int FLAG_FORCE_DARK = 1;
    public static final int FLAG_FORCE_LIGHT = 2;
    public static final int FLAG_INVERT = 3;

    private static int currentTheme;

    public static void applyTheme(Activity activity) {
        currentTheme = getTheme();
        activity.setTheme(currentTheme);

        activity.getWindow().setFormat(PixelFormat.RGBA_8888);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    }

    public static int getTheme() {
        String preference = Preferences.getStringValue(R.string.p_theme);
        return getStyleForSetting(preference);
    }

    public static int getWidgetTheme() {
        String preference = Preferences.getStringValue(R.string.p_theme_widget);
        if (TextUtils.isEmpty(preference) || THEME_WIDGET_SAME_AS_APP.equals(preference))
            return getTheme();
        else if (THEME_WIDGET_LEGACY.equals(preference))
            return TasksWidget.THEME_LEGACY;
        else
            return getStyleForSetting(preference);
    }

    private static int getStyleForSetting(String setting) {
        if(THEME_BLACK.equals(setting))
            return R.style.Theme;
        else if(THEME_TRANSPARENT.equals(setting))
            return R.style.Theme_Transparent;
        else if(THEME_TRANSPARENT_WHITE.equals(setting))
            return R.style.Theme_TransparentWhite;
        else if (THEME_WHITE_RED.equals(setting))
            return R.style.Theme_White;
        else
            return R.style.Theme_White_Blue;
    }

    public static int getThemeColor() {
        int theme = getTheme();
        switch(theme) {
        case R.style.Theme:
        case R.style.Theme_Transparent:
            return R.color.blue_theme_color;
        case R.style.Theme_White:
        case R.style.Theme_TransparentWhite:
            return R.color.red_theme_color;
        case R.style.Theme_White_Blue:
        default:
            return R.color.dark_blue_theme_color;
        }
    }

    public static int getEditDialogTheme() {
        int themeSetting = ThemeService.getTheme();
        int theme;
        if (themeSetting == R.style.Theme || themeSetting == R.style.Theme_Transparent) {
            theme = R.style.TEA_Dialog;
        } else {
            theme = R.style.TEA_Dialog_White;
        }
        return theme;
    }

    public static int getDialogTheme() {
        int themeSetting = ThemeService.getTheme();
        int theme;
        if (themeSetting == R.style.Theme || themeSetting == R.style.Theme_Transparent) {
            theme = R.style.Theme_Dialog;
        } else {
            theme = R.style.Theme_Dialog_White;
        }
        return theme;
    }

    public static String getDialogTextColor() {
        if (AndroidUtilities.getSdkVersion() >= 11) {
            int theme = getTheme();
            if (theme == R.style.Theme || theme == R.style.Theme_Transparent)
                return "white";
            else
                return "black";
        } else {
            return "white";
        }
    }

    public static int getDrawable(int lightDrawable) {
        return getDrawable(lightDrawable, 0);
    }

    public static int getDrawable(int lightDrawable, int alter) {
        boolean darkTheme = currentTheme == R.style.Theme || currentTheme == R.style.Theme_Transparent;
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
                AndroidUtilities.isTabletSized(ContextManager.getContext()))
            return R.drawable.icn_menu_refresh_tablet;

        if(!darkTheme)
            return lightDrawable;

        switch(lightDrawable) {
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
        case R.drawable.icn_menu_plugins:
            return R.drawable.icn_menu_plugins_dark;
        case R.drawable.icn_menu_settings:
            return R.drawable.icn_menu_settings_dark;
        case R.drawable.icn_menu_support:
            return R.drawable.icn_menu_support_dark;
        case R.drawable.icn_menu_tutorial:
            return R.drawable.icn_menu_tutorial_dark;
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

        throw new RuntimeException("No theme drawable found for " +
                ContextManager.getResources().getResourceName(lightDrawable));
    }

    public static void forceTheme(int theme) {
        currentTheme = theme;
    }

}
