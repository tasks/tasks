package org.tasks.analytics;

import android.content.Context;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;
import io.fabric.sdk.android.Fabric;
import javax.inject.Inject;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

@ApplicationScope
public class Tracker {

  private static boolean enabled;

  @Inject
  public Tracker(@ForApplication Context context, Preferences preferences) {
    enabled = preferences.isTrackingEnabled();
    if (enabled) {
      FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
      analytics.setAnalyticsCollectionEnabled(true);
      Fabric.with(context, new Crashlytics());
    }
  }

  public void reportException(Throwable t) {
    Timber.e(t);
    if (enabled) {
      Crashlytics.logException(t);
    }
  }
}
