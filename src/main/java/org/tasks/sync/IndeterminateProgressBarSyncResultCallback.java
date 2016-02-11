package org.tasks.sync;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.Broadcaster;

public class IndeterminateProgressBarSyncResultCallback extends RecordSyncStatusCallback {

    private final TaskListFragment taskListFragment;
    private final GtasksPreferenceService gtasksPreferenceService;

    public IndeterminateProgressBarSyncResultCallback(TaskListFragment taskListFragment, GtasksPreferenceService gtasksPreferenceService, Broadcaster broadcaster) {
        super(gtasksPreferenceService, broadcaster);
        this.taskListFragment = taskListFragment;
        this.gtasksPreferenceService = gtasksPreferenceService;
    }

    @Override
    public void started() {
        super.started();

        taskListFragment.setSyncOngoing(gtasksPreferenceService.isOngoing());
    }

    @Override
    public void finished() {
        super.finished();

        taskListFragment.setSyncOngoing(gtasksPreferenceService.isOngoing());
    }
}
