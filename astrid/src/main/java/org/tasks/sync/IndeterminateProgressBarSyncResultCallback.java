package org.tasks.sync;

import android.support.v4.app.FragmentActivity;

import com.todoroo.astrid.sync.SyncProviderUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndeterminateProgressBarSyncResultCallback extends RecordSyncStatusCallback {

    private static final Logger log = LoggerFactory.getLogger(IndeterminateProgressBarSyncResultCallback.class);

    private final FragmentActivity activity;
    private Runnable onFinished;

    public IndeterminateProgressBarSyncResultCallback(SyncProviderUtilities syncProviderUtilities, FragmentActivity activity, Runnable onFinished) {
        super(syncProviderUtilities);

        this.activity = activity;
        this.onFinished = onFinished;
    }

    @Override
    public void started() {
        super.started();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    activity.setProgressBarIndeterminateVisibility(true);
                } catch (IllegalStateException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

    }

    @Override
    public void finished() {
        super.finished();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    activity.setProgressBarIndeterminateVisibility(false);
                    onFinished.run();
                } catch (IllegalStateException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }
}
