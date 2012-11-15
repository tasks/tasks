package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONObject;

public class AcknowledgeChange extends ServerToClientMessage {

    public AcknowledgeChange(JSONObject json) {
        super(json);
        throw new RuntimeException("No constructor for AcknowledgeChange implemented"); //$NON-NLS-1$
    }

    @Override
    public void processMessage() {
        // TODO Auto-generated method stub
    }

}
