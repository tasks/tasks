package org.tasks.preferences;

import static java.util.Arrays.asList;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.speech.RecognizerIntent;
import com.google.common.base.Joiner;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.List;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.locale.Locale;
import timber.log.Timber;

public class Device {

  private final Context context;
  private final Locale locale;

  @Inject
  public Device(@ApplicationContext Context context, Locale locale) {
    this.context = context;
    this.locale = locale;
  }

  public boolean hasCamera() {
    return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
  }

  public boolean hasMicrophone() {
    return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
  }

  public boolean voiceInputAvailable() {
    PackageManager pm = context.getPackageManager();
    List<ResolveInfo> activities =
        pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
    return (activities.size() != 0);
  }

  public String getDebugInfo() {
    try {
      java.util.Locale appLocale = locale.getLocale();
      java.util.Locale deviceLocale = locale.getDeviceLocale();
      return Joiner.on("\n")
          .join(
              asList(
                  "",
                  "----------",
                  "Tasks: "
                      + BuildConfig.VERSION_NAME
                      + " ("
                      + BuildConfig.FLAVOR
                      + " build "
                      + BuildConfig.VERSION_CODE
                      + ")",
                  "Android: " + Build.VERSION.RELEASE + " (" + Build.DISPLAY + ")",
                  "Locale: "
                      + deviceLocale
                      + (!deviceLocale.equals(appLocale) ? " (" + appLocale + ")" : ""),
                  "Model: " + Build.MANUFACTURER + " " + Build.MODEL,
                  "Product: " + Build.PRODUCT + " (" + Build.DEVICE + ")",
                  "Kernel: "
                      + System.getProperty("os.version")
                      + " ("
                      + Build.VERSION.INCREMENTAL
                      + ")",
                  "----------",
                  ""));
    } catch (Exception e) {
      Timber.e(e);
    }
    return "";
  }
}
