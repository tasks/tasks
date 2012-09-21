package com.todoroo.astrid.actfm.sync.messages;

import com.todoroo.astrid.data.RemoteModel;

public class RequestDoubleCheck<TYPE extends RemoteModel> implements ClientToServerMessage {

    private final Class<? extends RemoteModel> modelClass;
    private final long uuid;

    public RequestDoubleCheck(TYPE entity) {
        this.modelClass = entity.getClass();
        this.uuid = entity.getValue(RemoteModel.REMOTE_ID_PROPERTY);
    }

    public void sendMessage() {
        // Send message
    }
}
