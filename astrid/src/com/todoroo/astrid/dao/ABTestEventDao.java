/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.ABTestEvent;

public class ABTestEventDao extends DatabaseDao<ABTestEvent> {

    @Autowired
    private Database database;

    public ABTestEventDao() {
        super(ABTestEvent.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    /**
     * Creates the baseline +0 days event (i.e. the first time the user
     * launches the app containing this test)
     * @param testName - the name of the test
     * @param testVariant - which option was chosen for this user
     * @param newUser - are they a new user?
     * @param activeUser - are they an activated user?
     */
    public void createInitialTestEvent(String testName, String testVariant,
            boolean newUser, boolean activeUser) {
        ABTestEvent event = new ABTestEvent();
        event.setValue(ABTestEvent.TEST_NAME, testName);
        event.setValue(ABTestEvent.TEST_VARIANT, testVariant);
        event.setValue(ABTestEvent.NEW_USER, newUser ? 1 : 0);
        event.setValue(ABTestEvent.ACTIVATED_USER, activeUser ? 1 : 0);
        event.setValue(ABTestEvent.TIME_INTERVAL, 0);
        event.setValue(ABTestEvent.DATE_RECORDED, DateUtilities.now());

        createNew(event);
    }

    /**
     * Only public for unit testing--don't use unless you really mean it!
     *
     * Creates data points for the specified test name, creating one data point
     * for each time interval that hasn't yet been recorded up to the specified one
     * @param testName
     * @param timeInterval
     */
    public void createTestEventWithTimeInterval(String testName, int timeInterval) {
        TodorooCursor<ABTestEvent> existing = query(Query.select(ABTestEvent.PROPERTIES)
                .where(ABTestEvent.TEST_NAME.eq(testName)).orderBy(Order.asc(ABTestEvent.TIME_INTERVAL)));

        try {
            if (existing.getCount() == 0)
                return;

            existing.moveToLast();
            ABTestEvent item = new ABTestEvent(existing);
            int lastRecordedTimeIntervalIndex = AndroidUtilities.indexOf(
                    ABTestEvent.TIME_INTERVALS, item.getValue(ABTestEvent.TIME_INTERVAL));

            int currentTimeIntervalIndex = AndroidUtilities.indexOf(
                    ABTestEvent.TIME_INTERVALS, timeInterval);

            if (lastRecordedTimeIntervalIndex < 0 || currentTimeIntervalIndex < 0)
                return;

            long now = DateUtilities.now();
            for (int i = lastRecordedTimeIntervalIndex + 1; i <= currentTimeIntervalIndex; i++) {
                item.clearValue(ABTestEvent.ID);
                item.setValue(ABTestEvent.REPORTED, 0);
                item.setValue(ABTestEvent.TIME_INTERVAL, ABTestEvent.TIME_INTERVALS[i]);
                item.setValue(ABTestEvent.DATE_RECORDED, now);
                createNew(item);
            }
        } finally {
            existing.close();
        }
        return;
    }

    /**
     * For each baseline data point that exists in the database, check the current
     * time against the time that baseline was recorded and report the appropriate
     * +n days events. Called on startup.
     */
    public void createRelativeDateEvents() {
        TodorooCursor<ABTestEvent> allEvents = query(Query.select(ABTestEvent.TEST_NAME, ABTestEvent.DATE_RECORDED)
                .where(ABTestEvent.TIME_INTERVAL.eq(0)));

        try {
            long now = DateUtilities.now();
            ABTestEvent event = new ABTestEvent();
            for (allEvents.moveToFirst(); !allEvents.isAfterLast(); allEvents.moveToNext()) {
                event.readFromCursor(allEvents);
                long baseTime = event.getValue(ABTestEvent.DATE_RECORDED);
                long timeSinceBase = now - baseTime;

                String testName = event.getValue(ABTestEvent.TEST_NAME);
                int timeInterval = -1;
                long days = timeSinceBase / DateUtilities.ONE_DAY;
                for(int i = 0; i < ABTestEvent.TIME_INTERVALS.length; i++)
                    if(days >= ABTestEvent.TIME_INTERVALS[i])
                        timeInterval = ABTestEvent.TIME_INTERVALS[i];

                if (timeInterval > 0)
                    createTestEventWithTimeInterval(testName, timeInterval);
            }
        } finally {
            allEvents.close();
        }
    }

}
