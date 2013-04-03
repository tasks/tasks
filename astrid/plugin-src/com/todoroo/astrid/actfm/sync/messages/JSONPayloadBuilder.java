package com.todoroo.astrid.actfm.sync.messages;

import org.apache.http.entity.mime.MultipartEntity;
import org.json.JSONObject;

import com.todoroo.astrid.actfm.sync.ActFmSyncThread;

public class JSONPayloadBuilder {

    private final StringBuilder sb = new StringBuilder("["); //$NON-NLS-1$
    private final StringBuilder temp = new StringBuilder();

    private int messageCount = 0;

    public boolean addMessage(ClientToServerMessage<?> message, MultipartEntity entity) {
        try {
            JSONObject serialized = message.serializeToJSON(entity);
            return addJSONObject(serialized);
        } catch (OutOfMemoryError e) {
            return false;
        }
    }

    public boolean addJSONObject(JSONObject obj) {
        try {
            temp.delete(0, temp.length());
            if (obj != null) {
                temp.append(obj)
                .append(","); //$NON-NLS-1$
                ActFmSyncThread.syncLog(temp.toString());

                sb.append(temp);

                messageCount++;
                return true;
            } else {
                return false;
            }
        } catch (OutOfMemoryError e) {
            return false;
        }
    }

    public int getMessageCount() {
        return messageCount;
    }

    public String closeAndReturnString() {
        if (messageCount > 0)
            sb.deleteCharAt(sb.length() - 1); // Remove final comma
        sb.append("]"); //$NON-NLS-1$
        return sb.toString();
    }

}
