package com.todoroo.astrid.service;

import android.content.Context;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.widget.TasksWidget;

public class WidgetUpdatingCallbackWrapper implements SyncResultCallback {

    private final Context context;
    private SyncResultCallback wrap;

    public WidgetUpdatingCallbackWrapper(Context context, SyncResultCallback wrap) {
        this.context = context;
        this.wrap = wrap;
    }

    @Override
    public void started() {
        wrap.started();
        TasksWidget.suppressUpdateFlag = DateUtilities.now();
    }

    @Override
    public void finished() {
        wrap.finished();
        TasksWidget.suppressUpdateFlag = 0L;
        TasksWidget.updateWidgets(context);
    }
}
