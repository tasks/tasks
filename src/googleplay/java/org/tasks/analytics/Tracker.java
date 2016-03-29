package org.tasks.analytics;

import android.content.Context;

import com.android.vending.billing.IabResult;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
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
    private final StandardExceptionParser exceptionParser;
    private Context context;

    @Inject
    public Tracker(@ForApplication Context context) {
        this.context = context;
        analytics = GoogleAnalytics.getInstance(context);
        tracker = analytics.newTracker(R.xml.google_analytics);
        tracker.setAppVersion(Integer.toString(BuildConfig.VERSION_CODE));
        exceptionParser = new StandardExceptionParser(context, null);
        if (BuildConfig.DEBUG) {
            analytics.setDryRun(true);
        }
    }

    public void showScreen(String screenName) {
        tracker.setScreenName(screenName);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void setTrackingEnabled(boolean enabled) {
        analytics.setAppOptOut(!enabled);
    }

    public void reportException(Exception e) {
        tracker.send(new HitBuilders.ExceptionBuilder()
                .setDescription(exceptionParser.getDescription(Thread.currentThread().getName(), e))
                .setFatal(false)
                .build());
    }

    public void reportEvent(Tracking.Events event) {
        reportEvent(event, null);
    }

    public void reportEvent(Tracking.Events event, String label) {
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                .setCategory(context.getString(event.category))
                .setAction(context.getString(event.action));
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
