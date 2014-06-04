package org.tasks.sync;

import android.support.v4.app.FragmentActivity;

import com.todoroo.astrid.sync.SyncResultCallback;

public class IndeterminateProgressBarSyncResultCallback implements SyncResultCallback{

    private final FragmentActivity activity;
    private Runnable onFinished;

    public IndeterminateProgressBarSyncResultCallback(FragmentActivity activity, Runnable onFinished) {
        this.activity = activity;
        this.onFinished = onFinished;
    }

    @Override
    public void finished() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setProgressBarIndeterminateVisibility(false);
                onFinished.run();
            }
        });
    }

    @Override
    public void started() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setProgressBarIndeterminateVisibility(true);
            }
        });

    }
}
