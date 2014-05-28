/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import android.text.TextUtils;

import java.io.IOException;

public class GoogleTasksException extends IOException {

    private String type;

    public GoogleTasksException(String message, String type) {
        super(message);
        this.type = type;
    }

    public String getType() {
        if (!TextUtils.isEmpty(type)) {
            return type;
        }
        return getMessage();
    }
}
