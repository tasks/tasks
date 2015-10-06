package org.tasks.sync;

import org.tasks.time.DateTime;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SyncThrottle {

    private final Map<Long, DateTime> lastSync = new HashMap<>();

    @Inject
    public SyncThrottle() {
    }

    public synchronized boolean canSync(long listId) {
        DateTime now = new DateTime();
        DateTime last = lastSync.get(listId);
        lastSync.put(listId, now);
        return last == null || last.isBefore(now.minusMinutes(10));
    }
}
