package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONObject;

import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.data.WaitingOnMe;

@SuppressWarnings("nls")
public abstract class ServerToClientMessage {

    public abstract void processMessage(String serverTime);

    public static final String TYPE_MAKE_CHANGES = "MakeChanges";
    public static final String TYPE_NOW_BRIEFED = "NowBriefed";
    public static final String TYPE_ACKNOWLEDGE_CHANGE = "AcknowledgeChange";
    public static final String TYPE_USER_DATA = "UserData";
    public static final String TYPE_DOUBLE_CHECK = "DoubleCheck";
    public static final String TYPE_USER_MIGRATED = "UserMigrated";
    public static final String TYPE_DEBUG = "Debug";

    protected final JSONObject json;

    public ServerToClientMessage(JSONObject json) {
        this.json = json;
    }

    public static ServerToClientMessage instantiateMessage(JSONObject json) {
        String type = json.optString("type");
        if (TYPE_MAKE_CHANGES.equals(type))
            return instantiateMakeChanges(json);
        else if (TYPE_NOW_BRIEFED.equals(type))
            return instantiateNowBriefed(json);
        else if (TYPE_ACKNOWLEDGE_CHANGE.equals(type))
            return new AcknowledgeChange(json);
        else if (TYPE_USER_DATA.equals(type))
            return new UserData(json);
        else if (TYPE_DOUBLE_CHECK.equals(type))
            return new DoubleCheck(json);
        else if (TYPE_USER_MIGRATED.equals(type))
            return new UserMigrated(json);
        else if (TYPE_DEBUG.equals(type))
            return new Debug(json);

        return null;
    }

    private static MakeChanges<?> instantiateMakeChanges(JSONObject json) {
        String table = json.optString("table");
        if (NameMaps.TABLE_ID_TASKS.equals(table))
            return new MakeChanges<Task>(json, PluginServices.getTaskDao());
        else if (NameMaps.TABLE_ID_TAGS.equals(table))
            return new MakeChanges<TagData>(json, PluginServices.getTagDataDao());
        else if (NameMaps.TABLE_ID_USERS.equals(table))
            return new MakeChanges<User>(json, PluginServices.getUserDao());
        else if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(table))
            return new MakeChanges<UserActivity>(json, PluginServices.getUserActivityDao());
        else if (NameMaps.TABLE_ID_ATTACHMENTS.equals(table))
            return new MakeChanges<TaskAttachment>(json, PluginServices.getTaskAttachmentDao());
        else if (NameMaps.TABLE_ID_TASK_LIST_METADATA.equals(table))
            return new MakeChanges<TaskListMetadata>(json, PluginServices.getTaskListMetadataDao());
        else if (NameMaps.TABLE_ID_WAITING_ON_ME.equals(table))
            return new MakeChanges<WaitingOnMe>(json, PluginServices.getWaitingOnMeDao());
        else
            return null;
    }

    private static NowBriefed<?> instantiateNowBriefed(JSONObject json) {
        String table = json.optString("table");
        if (NameMaps.TABLE_ID_TASKS.equals(table))
            return new NowBriefed<Task>(json, PluginServices.getTaskDao());
        else if (NameMaps.TABLE_ID_TAGS.equals(table))
            return new NowBriefed<TagData>(json, PluginServices.getTagDataDao());
        else if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(table))
            return new NowBriefed<UserActivity>(json, PluginServices.getUserActivityDao());
        else if (NameMaps.TABLE_ID_USERS.equals(table))
            return new NowBriefed<User>(json, PluginServices.getUserDao());
        else if (NameMaps.TABLE_ID_ATTACHMENTS.equals(table))
            return new NowBriefed<TaskAttachment>(json, PluginServices.getTaskAttachmentDao());
        else if (NameMaps.TABLE_ID_TASK_LIST_METADATA.equals(table))
            return new NowBriefed<TaskListMetadata>(json, PluginServices.getTaskListMetadataDao());
        else if (NameMaps.TABLE_ID_WAITING_ON_ME.equals(table))
            return new NowBriefed<WaitingOnMe>(json, PluginServices.getWaitingOnMeDao());
        else
            return null;
    }

}
