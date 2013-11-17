package com.todoroo.astrid.service;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.widget.TasksWidget;

public class SyncResultCallbackWrapper implements SyncResultCallback {
    private final SyncResultCallback wrapped;
    public SyncResultCallbackWrapper(SyncResultCallback wrap) {
        this.wrapped = wrap;
    }
    @Override
    public void incrementMax(int incrementBy) {
        wrapped.incrementMax(incrementBy);
    }
    @Override
    public void incrementProgress(int incrementBy) {
        wrapped.incrementProgress(incrementBy);
    }
    @Override
    public void started() {
        wrapped.started();
    }
    @Override
    public void finished() {
        wrapped.finished();
    }

    public static class WidgetUpdatingCallbackWrapper extends SyncResultCallbackWrapper {

        public WidgetUpdatingCallbackWrapper(SyncResultCallback wrap) {
            super(wrap);
        }

        @Override
        public void started() {
            super.started();
            TasksWidget.suppressUpdateFlag = DateUtilities.now();
        }

        @Override
        public void finished() {
            super.finished();
            TasksWidget.suppressUpdateFlag = 0L;
            TasksWidget.updateWidgets(ContextManager.getContext());
        }

    }
}
