package com.todoroo.astrid.service;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.ABTestEventDao;
import com.todoroo.astrid.data.ABTestEvent;
import com.todoroo.astrid.service.abtesting.ABChooser;
import com.todoroo.astrid.service.abtesting.ABTests;
import com.todoroo.astrid.test.DatabaseTestCase;

public class ABTestingServiceTest extends DatabaseTestCase {

    @Autowired ABTestEventDao abTestEventDao;
    @Autowired ABChooser abChooser;
    @Autowired ABTests abTests;

    public void testReportInitialEventNewUser() {
        testInitialEvents(true, false);
    }

    public void testReportInitialEventExistingUser() {
        testInitialEvents(false, true);
    }

    private void testInitialEvents(boolean newUser, boolean activatedUser) {
        testInterval(newUser, activatedUser, 0);
    }

    public void testIntervalEventWithShortIntervalNewUser() {
        testIntervalEventWithShortInterval(true, false);
    }

    public void testIntervalEventWithShortIntervalExistingUser() {
        testIntervalEventWithShortInterval(false, true);
    }

    public void testIntervalEventWithLongIntervalNewUser() {
        testIntervalEventWithLongInterval(true, false);
    }

    public void testIntervalEventWithLongIntervalExistingUser() {
        testIntervalEventWithLongInterval(false, true);
    }

    private void testIntervalEventWithShortInterval(boolean newUser, boolean activatedUser) {
        testInterval(newUser, activatedUser, 3);
    }

    private void testIntervalEventWithLongInterval(boolean newUser, boolean activatedUser) {
        testInterval(newUser, activatedUser, 14);
    }

    private void testInterval(boolean newUser, boolean activatedUser, int testInterval) {
        abChooser.makeChoicesForAllTests(newUser, activatedUser);
        abTestEventDao.createTestEventWithTimeInterval(TEST_NAME, testInterval);

        TodorooCursor<ABTestEvent> events = abTestEventDao.query(
                Query.select(ABTestEvent.PROPERTIES)
                .where(ABTestEvent.TEST_NAME.eq(TEST_NAME))
                .orderBy(Order.asc(ABTestEvent.TIME_INTERVAL)));
        try {
            int maxIntervalIndex = AndroidUtilities.indexOf(ABTestEvent.TIME_INTERVALS, testInterval);
            assertEquals(maxIntervalIndex + 1, events.getCount());

            for (int i = 0; i < events.getCount(); i++) {
                events.moveToNext();
                ABTestEvent event = new ABTestEvent(events);
                assertExpectedValues(event, newUser, activatedUser, ABTestEvent.TIME_INTERVALS[i]);
            }
        } finally {
            events.close();
        }
    }

    private void assertExpectedValues(ABTestEvent event, boolean newUser, boolean activatedUser, int timeInterval) {
        assertEquals(TEST_NAME, event.getValue(ABTestEvent.TEST_NAME));
        assertEquals(newUser ? 1 : 0, event.getValue(ABTestEvent.NEW_USER).intValue());
        assertEquals(activatedUser ? 1 : 0, event.getValue(ABTestEvent.ACTIVATED_USER).intValue());
        assertEquals(timeInterval, event.getValue(ABTestEvent.TIME_INTERVAL).intValue());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        abTests.addTest(TEST_NAME, new int[] { 9, 1 } , new int[] { 1, 9 }, TEST_OPTIONS);
        Preferences.clear(TEST_NAME);
    }

    private static final String TEST_NAME = "test_experiment";
    private static final String[] TEST_OPTIONS = new String[] { "opt-1", "opt-2" };
}
