package org.tasks.sync;

import android.support.v4.app.FragmentActivity;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndeterminateProgressBarSyncResultCallback extends RecordSyncStatusCallback {

    private static final Logger log = LoggerFactory.getLogger(IndeterminateProgressBarSyncResultCallback.class);

    private final FragmentActivity activity;
    private Runnable onFinished;

    public IndeterminateProgressBarSyncResultCallback(GtasksPreferenceService gtasksPreferenceService, FragmentActivity activity, Runnable onFinished) {
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
                    log.error(e.getMessage(), e);
                }
            }
        });
    }
}
