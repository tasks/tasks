package org.tasks.locale.bundle;

import static com.google.common.base.Strings.isNullOrEmpty;

import android.os.Bundle;
import org.tasks.BuildConfig;
import timber.log.Timber;

public final class ListNotificationBundle {

  public static final String BUNDLE_EXTRA_STRING_FILTER = "org.tasks.locale.STRING_FILTER";
  public static final String BUNDLE_EXTRA_PREVIOUS_BUNDLE = "org.tasks.locale.PREVIOUS_BUNDLE";
  private static final String BUNDLE_EXTRA_INT_VERSION_CODE = "org.tasks.locale.INT_VERSION_CODE";

  private ListNotificationBundle() {}

  public static boolean isBundleValid(final Bundle bundle) {
    if (null == bundle) {
      Timber.e("bundle is null");
      return false;
    }

    if (bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, -1) == -1) {
      return false;
    }

    if (isNullOrEmpty(bundle.getString(BUNDLE_EXTRA_STRING_FILTER))) {
      Timber.e("Invalid %s", BUNDLE_EXTRA_STRING_FILTER);
      return false;
    }

    return true;
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
}
