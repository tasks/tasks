package org.tasks.preferences;

import android.content.Context;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import org.tasks.R;

public class Theme {
    private final Context context;
    private final int themeRes;
    private final int themeIndex;

    public Theme(Context context, int themeIndex, int themeRes) {
        this.themeIndex = themeIndex;
        this.context = new ContextThemeWrapper(context, themeRes);
        this.themeRes = themeRes;
    }

    @Deprecated
    public int getThemeIndex() {
        return themeIndex;
    }

    public int getPrimaryColor() {
        return resolveAttribute(R.attr.colorPrimary);
    }

    public int getAccentColor() {
        return resolveAttribute(R.attr.colorAccent);
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

    public int getResId() {
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
