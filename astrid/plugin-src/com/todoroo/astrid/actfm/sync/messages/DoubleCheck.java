package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONObject;

public class DoubleCheck extends ServerToClientMessage {

    public DoubleCheck(JSONObject json) {
        super(json);
        throw new RuntimeException("No constructor for DoubleCheck implemented"); //$NON-NLS-1$
    }

    @Override
    public void processMessage() {
        // TODO Auto-generated method stub
    }

}
