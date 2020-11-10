package org.tasks.notifications;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class NotificationLimiter {

  private final Queue<Long> queue = new LinkedList<>();
  private final int maxSize;
  private boolean summary = false;

  NotificationLimiter(int maxSize) {
    this.maxSize = maxSize;
  }

  synchronized List<Long> add(long id) {
    if (id == NotificationManager.SUMMARY_NOTIFICATION_ID) {
      summary = true;
    } else {
      remove(id);
      queue.add(id);
    }
    List<Long> evicted = new ArrayList<>();
    for (int i = 0 ; i < size() - maxSize ; i++) {
      evicted.add(queue.remove());
    }
    return evicted;
  }

  synchronized void remove(Iterable<Long> ids) {
    for (Long id : ids) {
      remove(id);
    }
  }

  synchronized void remove(long id) {
    if (id == NotificationManager.SUMMARY_NOTIFICATION_ID) {
      summary = false;
    } else {
      queue.remove(id);
    }
  }

  private int size() {
    return queue.size() + (summary ? 1 : 0);
  }
}
