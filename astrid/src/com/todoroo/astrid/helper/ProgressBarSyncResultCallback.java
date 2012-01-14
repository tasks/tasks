package com.todoroo.astrid.helper;

import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ProgressBar;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.service.SyncV2Service.SyncResultCallback;

public class ProgressBarSyncResultCallback implements SyncResultCallback {

    private final ProgressBar progressBar;
    private final Activity activity;
    private final Runnable onFinished;

    private final AtomicInteger providers = new AtomicInteger(0);

    public ProgressBarSyncResultCallback(Activity activity,
            int progressBarId, Runnable onFinished) {
        this.progressBar = (ProgressBar) activity.findViewById(progressBarId);
        this.activity = activity;
        this.onFinished = onFinished;
        progressBar.setProgress(0);
        progressBar.setMax(0);
    }

    @Override
    public void finished() {
        if(providers.decrementAndGet() == 0) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setMax(100);
                    progressBar.setProgress(100);
                    AlphaAnimation animation = new AlphaAnimation(1, 0);
                    animation.setFillAfter(true);
                    animation.setDuration(1000L);
                    progressBar.startAnimation(animation);

                    onFinished.run();
                }
            });
            new Thread() {
                @Override
                public void run() {
                    AndroidUtilities.sleepDeep(1000);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }.start();
        }
    }

    @Override
    public void incrementMax(final int incrementBy) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setMax(progressBar.getMax() + incrementBy);
            }
        });
    }

    @Override
    public void incrementProgress(final int incrementBy) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.incrementProgressBy(incrementBy);
            }
        });
    }

    @Override
    public void started() {
        if(providers.incrementAndGet() == 1) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.VISIBLE);
                    AlphaAnimation animation = new AlphaAnimation(0, 1);
                    animation.setFillAfter(true);
                    animation.setDuration(1000L);
                    progressBar.startAnimation(animation);
                }
            });
        }
    }
}