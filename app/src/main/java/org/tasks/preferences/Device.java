package org.tasks.preferences;

import static java.util.Arrays.asList;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import com.google.common.base.Joiner;
import java.util.List;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.locale.Locale;
import timber.log.Timber;

public class Device {

  private final Context context;
  private final Locale locale;

  @Inject
  public Device(@ForApplication Context context, Locale locale) {
    this.context = context;
    this.locale = locale;
  }

  public boolean hasCamera() {
    return context
            .getPackageManager()
            .queryIntentActivities(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), 0)
            .size()
        > 0;
  }

  public boolean hasGallery() {
    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    intent.setType("image/*");
    return intent.resolveActivity(context.getPackageManager()) != null;
  }

  public boolean supportsLocationServices() {
    return context.getResources().getBoolean(R.bool.location_enabled);
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
