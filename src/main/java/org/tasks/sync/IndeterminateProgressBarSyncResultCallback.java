package org.tasks.sync;

import android.app.Activity;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import timber.log.Timber;

public class IndeterminateProgressBarSyncResultCallback extends RecordSyncStatusCallback {

    private final Activity activity;
    private Runnable onFinished;

    public IndeterminateProgressBarSyncResultCallback(GtasksPreferenceService gtasksPreferenceService, Activity activity, Runnable onFinished) {
        super(gtasksPreferenceService);

        this.activity = activity;
        this.onFinished = onFinished;
    }

    @Override
    public void finished() {
        super.finished();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    onFinished.run();
                } catch (IllegalStateException e) {
                    Timber.e(e, e.getMessage());
                }
            }
        });
    }
}
