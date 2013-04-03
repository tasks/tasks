package com.todoroo.astrid.actfm.sync.messages;

import org.apache.http.entity.mime.MultipartEntity;
import org.json.JSONArray;
import org.json.JSONObject;

public class JSONPayloadArray extends JSONArray {

    private final StringBuilder sb = new StringBuilder("["); //$NON-NLS-1$

    private int messageCount = 0;

    public boolean addMessage(ClientToServerMessage<?> message, MultipartEntity entity) {
        try {
            JSONObject serialized = message.serializeToJSON(entity);
            if (serialized != null) {
                sb.append(serialized.toString())
                .append(","); //$NON-NLS-1$

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
