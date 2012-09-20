package com.todoroo.astrid.actfm.sync.messages;

import java.util.ArrayList;
import java.util.List;

import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;

public class ChangesHappened<TYPE extends RemoteModel> implements ClientToServerMessage {

    private final Class<? extends RemoteModel> modelClass;
    private final long id;
    private final long uuid;
    private final List<OutstandingEntry<TYPE>> changes;
    private long pushedAt;

    public ChangesHappened(TYPE entity) {
        this.modelClass = entity.getClass();
        this.id = entity.getId();
        this.uuid = entity.getValue(RemoteModel.REMOTE_ID_PROPERTY);
        this.changes = new ArrayList<OutstandingEntry<TYPE>>();
    }

}
