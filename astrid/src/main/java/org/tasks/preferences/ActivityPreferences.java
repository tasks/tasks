package org.tasks.preferences;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

import org.tasks.injection.ForActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ActivityPreferences extends Preferences {

    public static final int MIN_TABLET_WIDTH = 550;
    public static final int MIN_TABLET_HEIGHT = 800;

    @Inject
    public ActivityPreferences(@ForActivity Context context) {
        super(context);
    }

    public boolean useTabletLayout() {
        return isTabletSized(context);
    }

    /**
     * Returns true if the screen is large or xtra large
     */
    public static boolean isTabletSized(Context context) {
        if (context.getPackageManager().hasSystemFeature("com.google.android.tv")) { //$NON-NLS-1$
            return true;
        }
        int size = context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;

        if (size == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            return true;
        } else if (size == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            float width = metrics.widthPixels / metrics.density;
            float height = metrics.heightPixels / metrics.density;

            float effectiveWidth = Math.min(width, height);
            float effectiveHeight = Math.max(width, height);

            return (effectiveWidth >= MIN_TABLET_WIDTH && effectiveHeight >= MIN_TABLET_HEIGHT);
        } else {
            return false;
        }
    }
}
