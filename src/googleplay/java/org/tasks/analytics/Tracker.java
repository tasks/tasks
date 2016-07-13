package org.tasks.analytics;

import android.content.Context;

import com.android.vending.billing.IabResult;
import com.google.android.gms.analytics.ExceptionParser;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Strings;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.injection.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
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
        exceptionParser = new ExceptionParser() {
            @Override
            public String getDescription(String thread, Throwable throwable) {
                StringBuilder stack = new StringBuilder();
                for (StackTraceElement element : throwable.getStackTrace()) {
                    stack.append(element.toString()).append("\n");
                }
                return String.format("%s {%s} %s", throwable.getClass().getName(), thread, stack.toString());
            }
        };
        ExceptionReporter reporter = new ExceptionReporter(
                tracker,
                Thread.getDefaultUncaughtExceptionHandler(),
                context);
        reporter.setExceptionParser(exceptionParser);
        Thread.setDefaultUncaughtExceptionHandler(reporter);
    }

    public void showScreen(String screenName) {
        tracker.setScreenName(screenName);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void setTrackingEnabled(boolean enabled) {
        analytics.setAppOptOut(!enabled);
    }

    public void reportException(Throwable t) {
        reportException(Thread.currentThread(), t);
    }

    public void reportException(Thread thread, Throwable t) {
        tracker.send(new HitBuilders.ExceptionBuilder()
                .setDescription(exceptionParser.getDescription(thread.getName(), t))
                .setFatal(false)
                .build());
    }

    public void reportEvent(Tracking.Events event) {
        reportEvent(event, null);
    }

    public void reportEvent(Tracking.Events event, String label) {
        reportEvent(event.category, event.action, label);
    }

    public void reportEvent(Tracking.Events event, int action, String label) {
        reportEvent(event.category, action, label);
    }

    private void reportEvent(int category, int action, String label) {
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                .setCategory(context.getString(category))
                .setAction(context.getString(action));
        if (!Strings.isNullOrEmpty(label)) {
            eventBuilder.setLabel(label);
        }
        tracker.send(eventBuilder.build());
    }

    public void reportIabResult(IabResult result, String sku) {
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(context.getString(R.string.tracking_category_iab))
                .setAction(sku)
                .setLabel(result.getMessage())
                .build());
    }
}
