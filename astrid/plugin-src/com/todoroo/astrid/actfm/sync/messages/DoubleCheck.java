package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONObject;

public class DoubleCheck extends ServerToClientMessage {

    public DoubleCheck(JSONObject json) {
        super(json);
    }

    @Override
    public void processMessage(String serverTime) {
        // TODO Auto-generated method stub
    }

}
