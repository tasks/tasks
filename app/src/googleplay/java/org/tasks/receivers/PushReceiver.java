package org.tasks.receivers;

import com.todoroo.astrid.data.Task;

import javax.inject.Inject;

public class PushReceiver {

    private final GoogleTaskPusher googleTaskPusher;

    @Inject
    public PushReceiver(GoogleTaskPusher googleTaskPusher) {
        this.googleTaskPusher = googleTaskPusher;
    }

    public void push(Task task, Task original) {
        googleTaskPusher.push(task, original);
    }
}
