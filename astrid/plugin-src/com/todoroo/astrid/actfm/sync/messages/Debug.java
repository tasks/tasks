package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

public class Debug extends ServerToClientMessage {

    public Debug(JSONObject json) {
        super(json);
    }

    @Override
    @SuppressWarnings("nls")
    public void processMessage(String serverTime) {
        String message = json.optString("message");
        if (!TextUtils.isEmpty(message))
            Log.w("actfm-debug-message", message);
    }

}
