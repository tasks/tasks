package com.todoroo.astrid.service.abtesting;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.todoroo.andlib.data.DatabaseDao.ModelUpdateListener;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.ABTestEventDao;
import com.todoroo.astrid.data.ABTestEvent;

@SuppressWarnings("nls")
public final class ABTestEventReportingService {

    private static final String KEY_TEST = "test";
    private static final String KEY_VARIANT = "variant";
    private static final String KEY_NEW_USER = "new";
    private static final String KEY_ACTIVE_USER = "activated";
    private static final String KEY_DAYS = "days";
    private static final String KEY_DATE = "date";

    @Autowired
    private ABTestEventDao abTestEventDao;

    @Autowired
    private ABTestInvoker abTestInvoker;

    public ABTestEventReportingService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public void initialize() {
        abTestEventDao.addListener(new ModelUpdateListener<ABTestEvent>() {
            @Override
            public void onModelUpdated(ABTestEvent model) {
                if (model.getValue(ABTestEvent.REPORTED) == 1)
                    return;

                pushABTestEvent(model);
            }
        });
    }

    public void pushABTestEvent(final ABTestEvent model) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    JSONArray payload = new JSONArray().put(jsonFromABTestEvent(model));
                    abTestInvoker.post(payload);
                    model.setValue(ABTestEvent.REPORTED, 1);
                    abTestEventDao.saveExisting(model);
                } catch (JSONException e) {
                    handleException(e);
                } catch (IOException e) {
                    handleException(e);
                }
            };
         }).start();
    }

    public void pushAllUnreportedABTestEvents() {
        final TodorooCursor<ABTestEvent> unreported = abTestEventDao.query(Query.select(ABTestEvent.PROPERTIES)
                .where(ABTestEvent.REPORTED.eq(0)));
        if (unreported.getCount() > 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONArray payload = jsonArrayFromABTestEvents(unreported);
                        abTestInvoker.post(payload);
                        for (unreported.moveToFirst(); !unreported.isAfterLast(); unreported.moveToNext()) {
                            ABTestEvent model = new ABTestEvent(unreported);
                            model.setValue(ABTestEvent.REPORTED, 1);
                            abTestEventDao.saveExisting(model);
                        }
                    }  catch (JSONException e) {
                        handleException(e);
                    } catch (IOException e) {
                        handleException(e);
                    } finally {
                        unreported.close();
                    }
                }
            }).start();
        }
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
        payload.put(KEY_DAYS, model.getValue(ABTestEvent.TIME_INTERVAL));

        long date = model.getValue(ABTestEvent.DATE_RECORDED) / 1000L;
        payload.put(KEY_DATE, date);

        return payload;
    }

    private static JSONArray jsonArrayFromABTestEvents(TodorooCursor<ABTestEvent> events) throws JSONException {
        JSONArray result = new JSONArray();
        for (events.moveToFirst(); !events.isAfterLast(); events.moveToNext()) {
            ABTestEvent model = new ABTestEvent(events);
            result.put(jsonFromABTestEvent(model));
        }
        return result;
    }

}
