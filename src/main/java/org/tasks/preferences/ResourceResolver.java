package org.tasks.preferences;

import android.app.Activity;
import android.util.TypedValue;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Deprecated
public class ResourceResolver {

    private final Activity activity;

    @Inject
    public ResourceResolver(Activity activity) {
        this.activity = activity;
    }

    public int getData(int attr) {
        return getData(activity, attr);
    }

    public int getResource(int attr) {
        return getResource(activity, attr);
    }

    public static int getResource(Activity activity, int attr) {
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.resourceId;
    }

    public static int getData(Activity activity, int attr) {
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
