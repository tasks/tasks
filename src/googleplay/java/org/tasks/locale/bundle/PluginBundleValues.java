package org.tasks.locale.bundle;

import android.os.Bundle;

import org.tasks.BuildConfig;

import timber.log.Timber;

public final class PluginBundleValues {

    public static final String BUNDLE_EXTRA_STRING_FILTER = "org.tasks.locale.STRING_FILTER";
    public static final String BUNDLE_EXTRA_PREVIOUS_BUNDLE = "org.tasks.locale.PREVIOUS_BUNDLE";
    private static final String BUNDLE_EXTRA_INT_VERSION_CODE = "org.tasks.locale.INT_VERSION_CODE";

    public static boolean isBundleValid(final Bundle bundle) {
        if (null == bundle) {
            Timber.e("bundle is null");
            return false;
        }

        if (isNullOrEmpty(bundle, BUNDLE_EXTRA_STRING_FILTER)) {
            return false;
        }

        Integer version = bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, -1);
        if (version == -1) {
            Timber.e("invalid version code: %s", version);
            return false;
        }

        return true;
    }

    private static boolean isNullOrEmpty(Bundle bundle, String key) {
        String value = bundle.getString(key);
        boolean isNullOrEmpty = value == null || value.trim().length() == 0;
        if (isNullOrEmpty) {
            Timber.e("Invalid %s", key);
        }
        return isNullOrEmpty;
    }

    public static Bundle generateBundle(String filter) {
        Bundle result = new Bundle();
        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, BuildConfig.VERSION_CODE);
        result.putString(BUNDLE_EXTRA_STRING_FILTER, filter);
        return result;
    }

    public static String getFilter(Bundle bundle) {
        return bundle.getString(BUNDLE_EXTRA_STRING_FILTER);
    }

    private PluginBundleValues() {
    }
}
