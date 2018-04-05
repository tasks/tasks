// package com.yourcompany.yourcondition;
// package com.yourcompany.yoursetting;

package net.dinglisch.android.tasker;

// Constants and functions for Tasker *extensions* to the plugin protocol
// See Also: http://tasker.dinglisch.net/plugins.html

// Release Notes

// v1.1 20140202
// added function variableNameValid()
// fixed some javadoc entries (thanks to David Stone)

// v1.2 20140211
// added ACTION_EDIT_EVENT

// v1.3 20140227
// added REQUESTED_TIMEOUT_MS_NONE, REQUESTED_TIMEOUT_MS_MAX and REQUESTED_TIMEOUT_MS_NEVER
// requestTimeoutMS(): added range check

// v1.4 20140516
// support for data pass through in REQUEST_QUERY intent
// some javadoc entries fixed (thanks again David :-))

// v1.5 20141120
// added RESULT_CODE_FAILED_PLUGIN_FIRST
// added Setting.VARNAME_ERROR_MESSAGE

// v1.6 20150213
// added Setting.getHintTimeoutMS()
// added Host.addHintTimeoutMS()

// v1.7 20160619
// null check for getCallingActivity() in hostSupportsOnFireVariableReplacement( Activity
// editActivity )

// v1.8 20161002
// added hostSupportsKeyEncoding(), setKeyEncoding() and Host.getKeysWithEncoding()

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

@SuppressLint("LogNotTimber")
public class TaskerPlugin {

  /** @see Setting#hostSupportsOnFireVariableReplacement(Bundle) */
  private static final int EXTRA_HOST_CAPABILITY_SETTING_FIRE_VARIABLE_REPLACEMENT = 8;

  private static final String TAG = "TaskerPlugin";
  private static final String BASE_KEY = "net.dinglisch.android.tasker";
  private static final String EXTRAS_PREFIX = BASE_KEY + ".extras.";
  private static final int FIRST_ON_FIRE_VARIABLES_TASKER_VERSION = 80;
  /** Host capabilities, passed to plugin with edit intents */
  private static final String EXTRA_HOST_CAPABILITIES = EXTRAS_PREFIX + "HOST_CAPABILITIES";

  private static Object getBundleValueSafe(
      Bundle b, String key, Class<?> expectedClass, String funcName) {
    Object value = null;

    if (b != null) {
      if (b.containsKey(key)) {
        Object obj = b.get(key);
        if (obj == null) {
          Log.w(TAG, funcName + ": " + key + ": null value");
        } else if (obj.getClass() != expectedClass) {
          Log.w(
              TAG,
              funcName
                  + ": "
                  + key
                  + ": expected "
                  + expectedClass.getClass().getName()
                  + ", got "
                  + obj.getClass().getName());
        } else {
          value = obj;
        }
      }
    }
    return value;
  }

  // ----------------------------- SETTING PLUGIN ONLY --------------------------------- //

  private static boolean hostSupports(Bundle extrasFromHost, int capabilityFlag) {
    Integer flags =
        (Integer)
            getBundleValueSafe(
                extrasFromHost, EXTRA_HOST_CAPABILITIES, Integer.class, "hostSupports");
    return (flags != null) && ((flags & capabilityFlag) > 0);
  }

  // ---------------------------------- HELPER FUNCTIONS -------------------------------- //

  private static int getPackageVersionCode(PackageManager pm, String packageName) {

    int code = -1;

    if (pm != null) {
      try {
        PackageInfo pi = pm.getPackageInfo(packageName, 0);
        if (pi != null) {
          code = pi.versionCode;
        }
      } catch (Exception e) {
        Log.e(TAG, "getPackageVersionCode: exception getting package info");
      }
    }

    return code;
  }

  private static void addStringArrayToBundleAsString(
      String[] toAdd, Bundle bundle, String key, String callerName) {

    StringBuilder builder = new StringBuilder();

    if (toAdd != null) {

      for (String keyName : toAdd) {

        if (keyName.contains(" ")) {
          Log.w(TAG, callerName + ": ignoring bad keyName containing space: " + keyName);
        } else {
          if (builder.length() > 0) {
            builder.append(' ');
          }

          builder.append(keyName);
        }

        if (builder.length() > 0) {
          bundle.putString(key, builder.toString());
        }
      }
    }
  }

  /**
   * Possible encodings of text in bundle values
   *
   * @see #setKeyEncoding(Bundle, String[], Encoding)
   */
  public enum Encoding {
    JSON
  }

  public static class Setting {

    /** @see #setVariableReplaceKeys(Bundle, String[]) */
    private static final String BUNDLE_KEY_VARIABLE_REPLACE_STRINGS =
        EXTRAS_PREFIX + "VARIABLE_REPLACE_KEYS";

    /**
     * Used by: plugin EditActivity.
     *
     * <p>Indicates to plugin that host will replace variables in specified bundle keys.
     *
     * <p>Replacement takes place every time the setting is fired, before the bundle is passed to
     * the plugin FireReceiver.
     *
     * @param extrasFromHost intent extras from the intent received by the edit activity
     * @see #setVariableReplaceKeys(Bundle, String[])
     */
    static boolean hostSupportsOnFireVariableReplacement(Bundle extrasFromHost) {
      return hostSupports(extrasFromHost, EXTRA_HOST_CAPABILITY_SETTING_FIRE_VARIABLE_REPLACEMENT);
    }

    /**
     * Used by: plugin EditActivity.
     *
     * <p>Description as above.
     *
     * <p>This version also includes backwards compatibility with pre 4.2 Tasker versions. At some
     * point this function will be deprecated.
     *
     * @param editActivity the plugin edit activity, needed to test calling Tasker version
     * @see #setVariableReplaceKeys(Bundle, String[])
     */
    public static boolean hostSupportsOnFireVariableReplacement(Activity editActivity) {

      boolean supportedFlag =
          hostSupportsOnFireVariableReplacement(editActivity.getIntent().getExtras());

      if (!supportedFlag) {

        ComponentName callingActivity = editActivity.getCallingActivity();

        if (callingActivity == null) {
          Log.w(
              TAG,
              "hostSupportsOnFireVariableReplacement: null callingActivity, defaulting to false");
        } else {
          String callerPackage = callingActivity.getPackageName();

          // Tasker only supporteed this from 1.0.10
          supportedFlag =
              (callerPackage.startsWith(BASE_KEY))
                  && (getPackageVersionCode(editActivity.getPackageManager(), callerPackage)
                      > FIRST_ON_FIRE_VARIABLES_TASKER_VERSION);
        }
      }

      return supportedFlag;
    }

    /**
     * Used by: plugin EditActivity
     *
     * <p>Indicates to host which bundle keys should be replaced.
     *
     * @param resultBundleToHost the bundle being returned to the host
     * @param listOfKeyNames which bundle keys to replace variables in when setting fires
     * @see #hostSupportsOnFireVariableReplacement(Bundle)
     * @see #setKeyEncoding(Bundle, String[], Encoding)
     */
    public static void setVariableReplaceKeys(Bundle resultBundleToHost, String[] listOfKeyNames) {
      addStringArrayToBundleAsString(
          listOfKeyNames,
          resultBundleToHost,
          BUNDLE_KEY_VARIABLE_REPLACE_STRINGS,
          "setVariableReplaceKeys");
    }
  }
}
