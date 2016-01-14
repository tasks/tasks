package org.tasks.preferences;

import android.app.Activity;
import android.content.Context;
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

    public static int getData(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
