package org.tasks.analytics;

import static org.tasks.billing.BillingClient.BillingResponseToString;

import android.content.Context;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.google.android.gms.analytics.ExceptionParser;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.common.base.Strings;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import timber.log.Timber;

@ApplicationScope
public class Tracker {

  private final GoogleAnalytics analytics;
  private final com.google.android.gms.analytics.Tracker tracker;
  private final ExceptionParser exceptionParser;
  private final Context context;

  @Inject
  public Tracker(@ForApplication Context context) {
    this.context = context;
    analytics = GoogleAnalytics.getInstance(context);
    tracker = analytics.newTracker(R.xml.google_analytics);
    tracker.setAppVersion(Integer.toString(BuildConfig.VERSION_CODE));
    final StandardExceptionParser standardExceptionParser =
        new StandardExceptionParser(context, null);
    exceptionParser =
        (thread, throwable) -> {
          StringBuilder stack =
              new StringBuilder()
                  .append(standardExceptionParser.getDescription(thread, throwable))
                  .append("\n")
                  .append(throwable.getClass().getName())
                  .append("\n");
          for (StackTraceElement element : throwable.getStackTrace()) {
            stack.append(element.toString()).append("\n");
          }
          return stack.toString();
        };
    ExceptionReporter reporter =
        new ExceptionReporter(tracker, Thread.getDefaultUncaughtExceptionHandler(), context);
    reporter.setExceptionParser(exceptionParser);
    Thread.setDefaultUncaughtExceptionHandler(reporter);
  }

  public void setTrackingEnabled(boolean enabled) {
    analytics.setAppOptOut(!enabled);
  }

  public void reportException(Throwable t) {
    reportException(Thread.currentThread(), t);
  }

  public void reportException(Thread thread, Throwable t) {
    Timber.e(t, t.getMessage());
    tracker.send(
        new HitBuilders.ExceptionBuilder()
            .setDescription(exceptionParser.getDescription(thread.getName(), t))
            .setFatal(false)
            .build());
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

  private void reportEvent(int category, String action, String label) {
    HitBuilders.EventBuilder eventBuilder =
        new HitBuilders.EventBuilder().setCategory(context.getString(category)).setAction(action);
    if (!Strings.isNullOrEmpty(label)) {
      eventBuilder.setLabel(label);
    }
    tracker.send(eventBuilder.build());
  }

  public void reportIabResult(@BillingResponse int response, String sku) {
    tracker.send(
        new HitBuilders.EventBuilder()
            .setCategory(context.getString(R.string.tracking_category_iab))
            .setAction(sku)
            .setLabel(BillingResponseToString(response))
            .build());
  }
}
