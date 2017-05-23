package org.tasks.analytics;

import javax.inject.Inject;

import timber.log.Timber;

public class Tracker {

    @Inject
    public Tracker() {

    }

    public void setTrackingEnabled(boolean enabled) {

    }

    public void reportException(Throwable t) {
        Timber.e(t, t.getMessage());
    }

    public void reportException(Thread thread, Throwable t) {
        Timber.e(t, t.getMessage());
    }

    public void reportEvent(Tracking.Events event) {

    }

    public void reportEvent(Tracking.Events event, String string) {

    }

    public void reportEvent(Tracking.Events setPreference, int resId, String s) {

    }

    public void reportEvent(Tracking.Events category, String action, String label) {

    }
}
