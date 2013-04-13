package com.todoroo.astrid.actfm.sync.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.actfm.sync.ActFmInvoker;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.SyncMessageCallback;
import com.todoroo.astrid.dao.HistoryDao;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.History;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;

public class FetchHistory<TYPE extends RemoteModel> {

    private static final String ERROR_TAG = "actfm-fetch-history"; //$NON-NLS-1$

    private final RemoteModelDao<TYPE> dao;
    private final LongProperty historyTimeProperty;
    private final IntegerProperty historyHasMoreProperty;
    private final String table;
    private final String uuid;
    private final String taskTitle;
    private final long modifiedAfter;
    private final int offset;
    private final SyncMessageCallback done;

    @Autowired
    private ActFmInvoker actFmInvoker;

    @Autowired
    private HistoryDao historyDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    public FetchHistory(RemoteModelDao<TYPE> dao, LongProperty historyTimeProperty, IntegerProperty historyHasMoreProperty,
            String table, String uuid, String taskTitle, long modifiedAfter, int offset, SyncMessageCallback done) {
        DependencyInjectionService.getInstance().inject(this);
        this.dao = dao;
        this.historyTimeProperty = historyTimeProperty;
        this.historyHasMoreProperty = historyHasMoreProperty;
        this.table = table;
        this.uuid = uuid;
        this.taskTitle = taskTitle;
        this.modifiedAfter = modifiedAfter;
        this.offset = offset;
        this.done = done;
    }

    @SuppressWarnings("nls")
    public void execute() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String token = actFmPreferenceService.getToken();
                if (TextUtils.isEmpty(token) || TextUtils.isEmpty(uuid))
                    return;

                ArrayList<Object> params = new ArrayList<Object>();
                if (NameMaps.TABLE_ID_TASKS.equals(table))
                    params.add("task_id");
                else if (NameMaps.TABLE_ID_TAGS.equals(table))
                    params.add("tag_id");
                else
                    return;

                params.add(uuid);

                if (modifiedAfter > 0) {
                    params.add("modified_after"); params.add(modifiedAfter / 1000L);
                }

                if (offset > 0) {
                    params.add("offset"); params.add(offset);
                }

                params.add("token"); params.add(token);
                try {
                    JSONObject result = actFmInvoker.invoke("model_history_list", params.toArray(new Object[params.size()]));
                    JSONArray list = result.optJSONArray("list");
                    boolean hasMore = result.optInt("has_more") > 0;
                    long time = result.optLong("time") * 1000;
                    if (hasMore && offset == 0) {
                        historyDao.deleteWhere(History.TARGET_ID.eq(uuid));
                    }
                    if (list != null) {
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject historyJson = list.optJSONObject(i);
                            if (historyJson != null) {
                                History history = new History();
                                history.setValue(History.TABLE_ID, table);
                                history.setValue(History.TARGET_ID, uuid);
                                history.setValue(History.UUID, historyJson.optString("id") + ":" + uuid);

                                String userId = historyJson.optString("user_id");
                                if (userId.equals(ActFmPreferenceService.userId()))
                                    userId = Task.USER_ID_SELF;
                                history.setValue(History.USER_UUID, historyJson.optString("user_id"));
                                history.setValue(History.COLUMN, historyJson.optString("column"));
                                history.setValue(History.OLD_VALUE, historyJson.optString("prev"));
                                history.setValue(History.NEW_VALUE, historyJson.optString("value"));
                                history.setValue(History.CREATED_AT, historyJson.optLong("created_at") * 1000);

                                JSONArray taskObj = historyJson.optJSONArray("task");
                                if (taskObj != null) {
                                    history.setValue(History.TABLE_ID, NameMaps.TABLE_ID_TASKS);
                                    history.setValue(History.TARGET_ID, taskObj.optString(0));
                                    history.setValue(History.TASK, taskObj.toString());
                                } else if (NameMaps.TABLE_ID_TASKS.equals(table) && !TextUtils.isEmpty(taskTitle)) {
                                    taskObj = new JSONArray();
                                    taskObj.put(uuid);
                                    taskObj.put(taskTitle);
                                    history.setValue(History.TASK, taskObj.toString());
                                }

                                if (NameMaps.TABLE_ID_TAGS.equals(table)) {
                                    history.setValue(History.TAG_ID, uuid);
                                }

                                if (historyDao.update(History.UUID.eq(history.getValue(History.UUID)), history) <= 0) {
                                    historyDao.createNew(history);
                                }
                            }
                        }
                        if (time > 0) {
                            TYPE template;
                            try {
                                template = dao.getModelClass().newInstance();
                                template.setValue(historyTimeProperty, time);
                                if (modifiedAfter == 0 || hasMore)
                                    template.setValue(historyHasMoreProperty, hasMore ? 1 : 0);
                                dao.update(RemoteModel.UUID_PROPERTY.eq(uuid), template);
                            } catch (InstantiationException e) {
                                Log.e(ERROR_TAG, "Error instantiating model for recording time", e);
                            } catch (IllegalAccessException e) {
                                Log.e(ERROR_TAG, "Error instantiating model for recording time", e);
                            }
                        }
                    }

                    JSONObject users = result.optJSONObject("users");
                    if (users != null) {
                        Iterator<String> keys = users.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            JSONObject userObj = users.optJSONObject(key);
                            if (userObj != null) {
                                String userUuid = userObj.optString("id");
                                if (RemoteModel.isUuidEmpty(uuid))
                                    continue;

                                User user = new User();
                                user.setValue(User.FIRST_NAME, userObj.optString("first_name"));
                                user.setValue(User.LAST_NAME, userObj.optString("last_name"));
                                user.setValue(User.NAME, userObj.optString("name"));
                                user.setValue(User.PICTURE, userObj.optString("picture"));
                                user.setValue(User.UUID, userUuid);

                                if (userDao.update(User.UUID.eq(userUuid), user) <= 0) {
                                    userDao.createNew(user);
                                }
                            }
                        }
                    }

                } catch (IOException e) {
                    Log.e(ERROR_TAG, "Error getting model history", e);
                }

                if (done != null)
                    done.runOnSuccess();
            }
        }).start();
    }

}
