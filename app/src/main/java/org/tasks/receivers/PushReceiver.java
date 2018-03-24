package org.tasks.receivers;

import com.todoroo.astrid.data.Task;
import javax.inject.Inject;

public class PushReceiver {

  private final GoogleTaskPusher googleTaskPusher;
  private final CalDAVPushReceiver calDAVPushReceiver;

  @Inject
  public PushReceiver(GoogleTaskPusher googleTaskPusher, CalDAVPushReceiver calDAVPushReceiver) {
    this.googleTaskPusher = googleTaskPusher;
    this.calDAVPushReceiver = calDAVPushReceiver;
  }

  public void push(Task task, Task original) {
    googleTaskPusher.push(task, original);
    calDAVPushReceiver.push(task, original);
  }
}
