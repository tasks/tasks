package com.todoroo.astrid.actfm.sync.messages;

import java.text.ParseException;

import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;

@SuppressWarnings("nls")
public class NowBriefed<TYPE extends RemoteModel> extends ServerToClientMessage {

    private static final String ERROR_TAG = "actfm-now-briefed";

    private final RemoteModelDao<TYPE> dao;
    private final String table;
    private final String uuid;
    private final String taskId;
    private final String tagId;
    private final String userId;
    private long pushedAt;

    public NowBriefed(JSONObject json, RemoteModelDao<TYPE> dao) {
        super(json);
        this.table = json.optString("table");
        this.uuid = json.optString("uuid");
        this.taskId = json.optString(BriefMe.TASK_ID_KEY);
        this.tagId = json.optString(BriefMe.TAG_ID_KEY);
        this.userId = json.optString(BriefMe.USER_ID_KEY);
        this.dao = dao;
        try {
            this.pushedAt = DateUtilities.parseIso8601(json.optString("pushed_at"));
        } catch (ParseException e) {
            this.pushedAt = 0;
        }
    }

    @Override
    public void processMessage(String serverTime) {
        if (pushedAt > 0) {
            if (TextUtils.isEmpty(uuid)) {
                if (!TextUtils.isEmpty(taskId)) {
                    Task template = new Task();
                    if (NameMaps.TABLE_ID_ATTACHMENTS.equals(table))
                        template.setValue(Task.ATTACHMENTS_PUSHED_AT, pushedAt);
                    else if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(table))
                        template.setValue(Task.USER_ACTIVITIES_PUSHED_AT, pushedAt);

                    if (template.getSetValues() != null)
                        PluginServices.getTaskDao().update(Task.UUID.eq(taskId), template);

                } else if (!TextUtils.isEmpty(tagId)) {
                    TagData template = new TagData();
                    if (NameMaps.TABLE_ID_TASKS.equals(table))
                        template.setValue(TagData.TASKS_PUSHED_AT, pushedAt);
                    if (NameMaps.TABLE_ID_TASK_LIST_METADATA.equals(table))
                        template.setValue(TagData.METADATA_PUSHED_AT, pushedAt);
                    else if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(table))
                        template.setValue(TagData.USER_ACTIVITIES_PUSHED_AT, pushedAt);

                    if (template.getSetValues() != null)
                        PluginServices.getTagDataDao().update(TagData.UUID.eq(tagId), template);

                } else if (!TextUtils.isEmpty(userId)) {
                    if (NameMaps.TABLE_ID_TASKS.equals(table)) {
                        User template = new User();
                        template.setValue(User.TASKS_PUSHED_AT, pushedAt);
                        PluginServices.getUserDao().update(User.UUID.eq(userId), template);
                    }
                } else {
                    String pushedAtKey = null;
                    if (NameMaps.TABLE_ID_TASKS.equals(table))
                        pushedAtKey = NameMaps.PUSHED_AT_TASKS;
                    else if (NameMaps.TABLE_ID_TAGS.equals(table))
                        pushedAtKey = NameMaps.PUSHED_AT_TAGS;
                    else if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(table))
                        pushedAtKey = NameMaps.PUSHED_AT_ACTIVITY;
                    else if (NameMaps.TABLE_ID_USERS.equals(table))
                        pushedAtKey = NameMaps.PUSHED_AT_USERS;
                    else if (NameMaps.TABLE_ID_TASK_LIST_METADATA.equals(table))
                        pushedAtKey = NameMaps.PUSHED_AT_TASK_LIST_METADATA;
                    else if (NameMaps.TABLE_ID_WAITING_ON_ME.equals(table))
                        pushedAtKey = NameMaps.PUSHED_AT_WAITING_ON_ME;

                    if (pushedAtKey != null)
                        Preferences.setLong(pushedAtKey, pushedAt);
                }

            } else {
                try {
                    TYPE instance = dao.getModelClass().newInstance();
                    instance.setValue(RemoteModel.PUSHED_AT_PROPERTY, pushedAt);
                    dao.update(RemoteModel.UUID_PROPERTY.eq(uuid), instance);
                } catch (IllegalAccessException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for NowBriefed", e);
                } catch (InstantiationException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for NowBriefed", e);
                }
            }
        }
    }

}
