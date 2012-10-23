package com.zutubi.android.junitreport;

import java.io.File;
import java.lang.reflect.Method;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * Utilities for backwards-compatibility with early Android versions.
 */
public final class Compatibility {
    private static final String LOG_TAG = Compatibility.class.getSimpleName();

    private static final Method METHOD_GET_EXTERNAL_FILES_DIR;
    static {
        Method method = null;
        try {
            method = Context.class.getMethod("getExternalFilesDir", String.class);
        } catch (Exception e) {
            // Expected for API 7 and below.  Fall back will be engaged.
        }
        METHOD_GET_EXTERNAL_FILES_DIR = method;
    }

    /**
     * Do not instantiate.
     */
    private Compatibility() {
    }

    /**
     * A backwards-compatible version of {@link Context#getExternalFilesDir(String)}
     * which falls back to using {@link Environment#getExternalStorageDirectory()}
     * on API 7 and below.
     * 
     * @param context context to get the external files directory for
     * @param type the type of files directory to return (may be null)
     * @return the path of the directory holding application files on external
     *         storage, or null if external storage cannot be accessed
     */
    public static File getExternalFilesDir(final Context context, final String type) {
        if (METHOD_GET_EXTERNAL_FILES_DIR == null) {
            final File externalRoot = Environment.getExternalStorageDirectory();
            if (externalRoot == null) {
                return null;
            }

            final String packageName = context.getApplicationContext().getPackageName();
            return new File(externalRoot, "Android/data/" + packageName + "/files");
        } else {
            try {
                return (File) METHOD_GET_EXTERNAL_FILES_DIR.invoke(context, type);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Could not invoke getExternalFilesDir: " + e.getMessage(), e);
                return null;
            }
        }
    }
}
