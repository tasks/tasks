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
    // Counted once, before anything is removed. Re-read each time round, size() shrinks while i
    // grows, so the loop met in the middle and evicted half of what it should have: an excess of
    // four left two notifications over the cap on screen and reported only two of the four to the
    // caller, which is what decides whether a reminder was delivered. Only ever one over today -
    // add() takes a single id - which is why nothing has seen it; it is one batched add away from
    // mattering.
    int excess = size() - maxSize;
    List<Long> evicted = new ArrayList<>();
    for (int i = 0 ; i < excess ; i++) {
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
