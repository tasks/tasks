package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONObject;

@SuppressWarnings("nls")
public abstract class ServerToClientMessage {

    public abstract void processMessage();

    private static final String TYPE_MAKE_CHANGES = "MakeChanges";
    private static final String TYPE_ACKNOWLEDGE_CHANGE = "AcknowledgeChange";
    private static final String TYPE_DOUBLE_CHECK = "DoubleCheck";
    private static final String TYPE_DEBUG = "Debug";

    @SuppressWarnings("unused")
    public ServerToClientMessage(JSONObject json) {
        //
    }

    public static ServerToClientMessage instantiateMessage(JSONObject json) {
        String type = json.optString("type");
        if (TYPE_MAKE_CHANGES.equals(type))
            return new MakeChanges(json);
        else if (TYPE_ACKNOWLEDGE_CHANGE.equals(type))
            return new AcknowledgeChange(json);
        else if (TYPE_DOUBLE_CHECK.equals(json))
            return new DoubleCheck(json);
        else if (TYPE_DEBUG.equals(json))
            return new Debug(json);

        return null;
    }

}
