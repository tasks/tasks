package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONObject;

public class MakeChanges extends ServerToClientMessage {

    public MakeChanges(JSONObject json) {
        super(json);
        throw new RuntimeException("No constructor for MakeChanges implemented"); //$NON-NLS-1$
    }

    @Override
    public void processMessage() {
        // TODO Auto-generated method stub
    }

}
