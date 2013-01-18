package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONObject;

import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;

@SuppressWarnings("nls")
public abstract class ServerToClientMessage {

    public abstract void processMessage();

    public static final String TYPE_MAKE_CHANGES = "MakeChanges";
    public static final String TYPE_USER_DATA = "UserData";
    public static final String TYPE_ACKNOWLEDGE_CHANGE = "AcknowledgeChange";
    public static final String TYPE_DOUBLE_CHECK = "DoubleCheck";
    public static final String TYPE_DEBUG = "Debug";

    protected final JSONObject json;

    public ServerToClientMessage(JSONObject json) {
        this.json = json;
    }

    public static ServerToClientMessage instantiateMessage(JSONObject json, long pushedAt) {
        String type = json.optString("type");
        if (TYPE_MAKE_CHANGES.equals(type))
            return instantiateMakeChanges(json, pushedAt);
        else if (TYPE_ACKNOWLEDGE_CHANGE.equals(type))
            return new AcknowledgeChange(json);
        else if (TYPE_USER_DATA.equals(type))
            return new UserData(json);
        else if (TYPE_DOUBLE_CHECK.equals(json))
            return new DoubleCheck(json);
        else if (TYPE_DEBUG.equals(json))
            return new Debug(json);

        return null;
    }

    private static MakeChanges<?> instantiateMakeChanges(JSONObject json, long pushedAt) {
        String table = json.optString("table");
        if (NameMaps.TABLE_ID_TASKS.equals(table))
            return new MakeChanges<Task>(json, PluginServices.getTaskDao(), pushedAt);
        else if (NameMaps.TABLE_ID_TAGS.equals(table))
            return new MakeChanges<TagData>(json, PluginServices.getTagDataDao(), pushedAt);
        else if (NameMaps.TABLE_ID_PUSHED_AT.equals(table))
            return new MakeChanges<RemoteModel>(json, null, 0);
        else
            return null;
    }

}
