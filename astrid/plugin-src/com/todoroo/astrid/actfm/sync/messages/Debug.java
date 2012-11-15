package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONObject;

public class Debug extends ServerToClientMessage {

    public Debug(JSONObject json) {
        super(json);
        throw new RuntimeException("No constructor for Debug implemented"); //$NON-NLS-1$
    }

    @Override
    public void processMessage() {
        // TODO Auto-generated method stub
    }

}
