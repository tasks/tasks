package org.tasks.analytics;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;

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

    @Inject
    public Tracker(@ForApplication Context context) {
        analytics = GoogleAnalytics.getInstance(context);
        tracker = analytics.newTracker(R.xml.analytics);
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
}
