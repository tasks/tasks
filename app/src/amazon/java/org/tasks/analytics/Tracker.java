package org.tasks.analytics;

import static org.tasks.billing.BillingClient.BillingResponseToString;

import android.content.Context;
import android.os.Bundle;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.analytics.FirebaseAnalytics.Event;
import com.google.firebase.analytics.FirebaseAnalytics.Param;
import io.fabric.sdk.android.Fabric;
import javax.inject.Inject;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

@ApplicationScope
public class Tracker {

  private static boolean enabled;

  private final FirebaseAnalytics analytics;
  private final Context context;

  @Inject
  public Tracker(@ForApplication Context context, Preferences preferences) {
    this.context = context;
    enabled = preferences.isTrackingEnabled();
    if (enabled) {
      analytics = FirebaseAnalytics.getInstance(context);
      Fabric.with(context, new Crashlytics());
    } else {
      analytics = null;
    }
  }

  public static void report(Throwable t) {
    Timber.e(t);
    if (enabled) {
      Crashlytics.logException(t);
    }
  }

  public void reportException(Throwable t) {
    report(t);
  }

  public void reportEvent(Tracking.Events event) {
    reportEvent(event, null);
  }

  public void reportEvent(Tracking.Events event, String label) {
    reportEvent(event, event.action, label);
  }

  public void reportEvent(Tracking.Events event, int action, String label) {
    reportEvent(event, context.getString(action), label);
  }

  public void reportEvent(Tracking.Events event, String action, String label) {
    reportEvent(event.category, action, label);
  }

  private void reportEvent(int categoryRes, String action, String label) {
    if (!enabled) {
      return;
    }
  }

  public void reportIabResult(@BillingResponse int response, String sku) {
    if (!enabled) {
      return;
    }

    Bundle bundle = new Bundle();
    bundle.putString(Param.ITEM_ID, sku);
    bundle.putString(Param.SUCCESS, BillingResponseToString(response));
    analytics.logEvent(Event.ECOMMERCE_PURCHASE, bundle);
  }
}
