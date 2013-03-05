/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.helper;

import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ProgressBar;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.sync.SyncResultCallbackAdapter;

public class ProgressBarSyncResultCallback extends SyncResultCallbackAdapter {

    private ProgressBar progressBar;
    private final Activity activity;
    private final Runnable onFinished;

    private final AtomicInteger providers = new AtomicInteger(0);

    public ProgressBarSyncResultCallback(Activity activity, Fragment fragment,
            int progressBarId, Runnable onFinished) {
        this.progressBar = (ProgressBar) fragment.getView().findViewById(progressBarId);
        this.activity = activity;
        this.onFinished = onFinished;

        if(progressBar == null)
            progressBar = new ProgressBar(activity);

        progressBar.setProgress(0);
        progressBar.setMax(0);
    }

    @Override
    public void finished() {
        if(providers.decrementAndGet() == 0) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        progressBar.setMax(100);
                        progressBar.setProgress(100);
                        AlphaAnimation animation = new AlphaAnimation(1, 0);
                        animation.setFillAfter(true);
                        animation.setDuration(1000L);
                        progressBar.startAnimation(animation);

                        onFinished.run();
                    } catch (Exception e) {
                        // ignore, view could have been destroyed
                    }
                }
            });
            new Thread() {
                @Override
                public void run() {
                    AndroidUtilities.sleepDeep(1000);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                progressBar.setVisibility(View.GONE);
                            } catch (Exception e) {
                                // ignore
                            }
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
                try {
                    progressBar.setMax(progressBar.getMax() + incrementBy);
                } catch (Exception e) {
                    // ignore
                }
            }
        });
    }

    @Override
    public void incrementProgress(final int incrementBy) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    progressBar.incrementProgressBy(incrementBy);
                } catch (Exception e) {
                    // ignore
                }
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
