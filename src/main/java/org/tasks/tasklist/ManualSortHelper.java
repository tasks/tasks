package org.tasks.tasklist;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;

public class ManualSortHelper {

    private final Resources resources;
    private final int minRowHeight;

    @Inject
    public ManualSortHelper(@ForApplication Context context) {
        resources = context.getResources();
        minRowHeight = computeMinRowHeight();
    }

    private int computeMinRowHeight() {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (metrics.density * 40);
    }

    public int getMinRowHeight() {
        return minRowHeight;
    }
}
