package org.tasks.analytics;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.injection.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Tracker {

    private final GoogleAnalytics analytics;
    private final com.google.android.gms.analytics.Tracker tracker;

    @Inject
    public Tracker(@ForApplication Context context) {
        analytics = GoogleAnalytics.getInstance(context);
        tracker = analytics.newTracker(R.xml.analytics);
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
}
