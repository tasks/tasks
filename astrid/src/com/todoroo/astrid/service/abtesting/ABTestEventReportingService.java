/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service.abtesting;

import java.io.IOException;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.ABTestEventDao;
import com.todoroo.astrid.data.ABTestEvent;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;

/**
 * Service to manage the reporting of launch events for AB testing.
 * On startup, queries the ABTestEvent database for unreported data
 * points, merges them into the expected JSONArray format, and
 * pushes them to the server.
 * @author Sam
 *
 */
@SuppressWarnings("nls")
public final class ABTestEventReportingService {

    private static final String KEY_TEST = "test";
    private static final String KEY_VARIANT = "variant";
    private static final String KEY_NEW_USER = "new";
    private static final String KEY_ACTIVE_USER = "activated";
    private static final String KEY_DAYS = "days";
    private static final String KEY_DATE = "date";

    private static final String KEY_INITIAL = "initial";
    private static final String KEY_ACTIVATION = "activation";

    private static final String PREF_REPORTED_ACTIVATION = "p_reported_activation";

    @Autowired
    private ABTestEventDao abTestEventDao;

    @Autowired
    private ABTestInvoker abTestInvoker;

    @Autowired
    private ABTests abTests;

    @Autowired
    private TaskService taskService;

    public ABTestEventReportingService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Called on startup from TaskListActivity. Creates any +n days
     * launch events that need to be recorded, and pushes all unreported
     * data to the server.
     */
    public void trackUserRetention(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    abTestEventDao.createRelativeDateEvents();
                    pushAllUnreportedABTestEvents();
//                    reportUserActivation();
                } catch (SQLiteException e) {
                    StartupService.handleSQLiteError(context, e);
                }
            }
        }).start();
    }

    private void pushAllUnreportedABTestEvents() {
        synchronized(ABTestEventReportingService.class) {
            if (StatisticsService.dontCollectStatistics())
                return;
            final TodorooCursor<ABTestEvent> unreported = abTestEventDao.query(Query.select(ABTestEvent.PROPERTIES)
                    .where(ABTestEvent.REPORTED.eq(0))
                    .orderBy(Order.asc(ABTestEvent.TEST_NAME), Order.asc(ABTestEvent.TIME_INTERVAL)));
            if (unreported.getCount() > 0) {
                try {
                    JSONArray payload = jsonArrayFromABTestEvents(unreported);
                    abTestInvoker.post(ABTestInvoker.AB_RETENTION_METHOD, payload);
                    ABTestEvent model = new ABTestEvent();
                    for (unreported.moveToFirst(); !unreported.isAfterLast(); unreported.moveToNext()) {
                        model.readFromCursor(unreported);
                        model.setValue(ABTestEvent.REPORTED, 1);
                        abTestEventDao.saveExisting(model);
                    }
                } catch (JSONException e) {
                    handleException(e);
                } catch (IOException e) {
                    handleException(e);
                } finally {
                    unreported.close();
                }
            }
        }
    }

    private void reportUserActivation() {
        synchronized (ABTestEventReportingService.class) {
            if (StatisticsService.dontCollectStatistics())
                return;
            if (Preferences.getBoolean(PREF_REPORTED_ACTIVATION, false) || !taskService.getUserActivationStatus())
                return;

            final TodorooCursor<ABTestEvent> variants = abTestEventDao.query(Query.select(ABTestEvent.PROPERTIES)
                    .groupBy(ABTestEvent.TEST_NAME));
            try {
                JSONArray payload = jsonArrayForActivationAnalytics(variants);
                abTestInvoker.post(ABTestInvoker.AB_ACTIVATION_METHOD, payload);
                Preferences.setBoolean(PREF_REPORTED_ACTIVATION, true);
            } catch (JSONException e) {
                handleException(e);
            } catch (IOException e) {
                handleException(e);
            } finally {
                variants.close();
            }
        }
    }

    public JSONArray getTestsWithVariantsArray() {
        JSONArray array = new JSONArray();
        Set<String> tests = abTests.getAllTestKeys();

        for (String key : tests) {
            array.put(key + ":" + abTests.getDescriptionForTestOption(key, ABChooser.readChoiceForTest(key)));
        }
        return array;
    }

    private void handleException(Exception e) {
        Log.e("analytics", "analytics-error", e);
    }

    private static JSONObject jsonFromABTestEvent(ABTestEvent model) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put(KEY_TEST, model.getValue(ABTestEvent.TEST_NAME));
        payload.put(KEY_VARIANT, model.getValue(ABTestEvent.TEST_VARIANT));
        payload.put(KEY_NEW_USER, model.getValue(ABTestEvent.NEW_USER) > 0 ? true : false);
        payload.put(KEY_ACTIVE_USER, model.getValue(ABTestEvent.ACTIVATED_USER) > 0 ? true : false);
        payload.put(KEY_DAYS, new JSONArray().put(model.getValue(ABTestEvent.TIME_INTERVAL)));

        long date = model.getValue(ABTestEvent.DATE_RECORDED) / 1000L;
        payload.put(KEY_DATE, date);

        return payload;
    }

    private static JSONArray jsonArrayFromABTestEvents(TodorooCursor<ABTestEvent> events) throws JSONException {
        JSONArray result = new JSONArray();

        String lastTestKey = "";
        JSONObject testAcc = null;
        ABTestEvent model = new ABTestEvent();
        for (events.moveToFirst(); !events.isAfterLast(); events.moveToNext()) {
            model.readFromCursor(events);
            if (!model.getValue(ABTestEvent.TEST_NAME).equals(lastTestKey)) {
                if (testAcc != null)
                    result.put(testAcc);
                testAcc = jsonFromABTestEvent(model);
                lastTestKey = model.getValue(ABTestEvent.TEST_NAME);
            } else {
                int interval = model.getValue(ABTestEvent.TIME_INTERVAL);
                if (testAcc != null) { // this should never happen, just stopping the compiler from complaining
                    JSONArray daysArray = testAcc.getJSONArray(KEY_DAYS);
                    daysArray.put(interval);
                }
            }

        }
        if (testAcc != null)
            result.put(testAcc);
        return result;
    }

    private static JSONArray jsonArrayForActivationAnalytics(TodorooCursor<ABTestEvent> events) throws JSONException {
        JSONArray result = new JSONArray();

        ABTestEvent model = new ABTestEvent();
        for (events.moveToFirst(); !events.isAfterLast(); events.moveToNext()) {
            model.readFromCursor(events);
            JSONObject event = new JSONObject();
            event.put(KEY_TEST, model.getValue(ABTestEvent.TEST_NAME));
            event.put(KEY_VARIANT, model.getValue(ABTestEvent.TEST_VARIANT));
            if (model.getValue(ABTestEvent.ACTIVATED_USER) > 0)
                event.put(KEY_INITIAL, true);
            else
                event.put(KEY_ACTIVATION, true);
            result.put(event);
        }
        return result;
    }

}
