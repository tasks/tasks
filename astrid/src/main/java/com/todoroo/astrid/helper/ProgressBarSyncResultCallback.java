/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.helper;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ProgressBar;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.sync.SyncResultCallbackAdapter;

import java.util.concurrent.atomic.AtomicInteger;

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

        if(progressBar == null) {
            progressBar = new ProgressBar(activity);
        }

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

    /**
     * This method helps rescaling a progress bar to increase the number of data points that form
     * the progress bar, but without reducing the % of progress already done.
     *
     * This is done by increasing the length of the progress bar and the amount of progress whilst
     * keeping the remaining amount of progress left remains the same. This code makes the
     * presumption that progress isn't reversed and only increases.
     *
     * @param p The current level of progress that has been made.
     * @param m The current total of data points which makes up the progress bar (the maximum).
     * @param x The number of data points to increase the progress bar by.
     * @return An integer array of two values - the first is the new value for progress and the
     *     second is the new value for the maximum.
     */
    public static int[] rescaleProgressValues(int p, int m, int x) {

        /**
         * If we have no progress yet, then a straight-forward increment of the max will suffice
         * here (and is necessary, the below code will break otherwise).
         */
        if (p == 0) {return new int[] {p, m + x};}

        /**
         * This is more of an error in caller code. We shouldn't be asked to rescale a progress
         * bar that has currently reached 100%, but hasn't been declared as "finished". But in
         * case this happens, and since we can't correctly rescale a progress bar that's at 100%,
         * we just allow it to move back.
         */
        if (p == m) {return new int[] {p, m + x};}

        // This is what does the magic!
        double m2d = ((double)(m*(m+x-p))) / (m-p);
        int m2 = (int)Math.floor(m2d);
        int p2 = m2 + p - m - x;
        return new int[] {p2, m2};

    }

    private void rescaleProgressBarBy(int x) {
        int[] new_values = rescaleProgressValues(progressBar.getProgress(), progressBar.getMax(), x);

        if (new_values[1] != -1) {progressBar.setMax(new_values[1]);}
        if (new_values[0] != -1) {progressBar.setProgress(new_values[0]);}
    }

    @Override
    public void incrementMax(final int incrementBy) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    rescaleProgressBarBy(incrementBy);
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
