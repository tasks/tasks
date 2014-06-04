package com.todoroo.astrid.service;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.widget.TasksWidget;

public class WidgetUpdatingCallbackWrapper implements SyncResultCallback {

    private SyncResultCallback wrap;

    public WidgetUpdatingCallbackWrapper(SyncResultCallback wrap) {
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
        TasksWidget.updateWidgets(ContextManager.getContext());
    }
}
