package org.tasks.preferences;

import android.content.Context;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;

import org.tasks.R;

public class Theme {
    private final Context context;
    private final int themeRes;
    private final int themeIndex;
    private final String name;

    public Theme(Context context, int themeIndex, int themeRes, String name) {
        this.themeIndex = themeIndex;
        this.name = name;
        this.context = new ContextThemeWrapper(context, themeRes);
        this.themeRes = themeRes;
    }

    public LayoutInflater getThemedLayoutInflater() {
        return (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public boolean isDark() {
        return themeIndex == 1;
    }

    public String getName() {
        return name;
    }

    public int getThemeIndex() {
        return themeIndex;
    }

    public int getPrimaryColor() {
        return resolveAttribute(R.attr.colorPrimary);
    }

    public int getContentBackground() {
        return resolveAttribute(R.attr.asContentBackground);
    }

    public int getPrimaryDarkColor() {
        return resolveAttribute(R.attr.colorPrimaryDark);
    }

    public int getTextColor() {
        return resolveAttribute(R.attr.asTextColor);
    }

    public int getDateTimePickerAccent() {
        return resolveAttribute(R.attr.asDateTimePickerAccent);
    }

    public int getAppThemeResId() {
        return themeRes;
    }

    public int getDialogThemeResId() {
        return resolveAttribute(R.attr.alertDialogTheme);
    }

    private int resolveAttribute(int attribute) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attribute, typedValue, true);
        return typedValue.data;
    }
}
