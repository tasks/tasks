/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import java.io.IOException;

import android.text.TextUtils;

public class GoogleTasksException extends IOException {
    private static final long serialVersionUID = -5585448790574862510L;

    private String type;

    public GoogleTasksException(String message, String type) {
        super(message);
        this.type = type;
    }

    public GoogleTasksException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    public GoogleTasksException(Throwable cause) {
        super(cause.getMessage());
        initCause(cause);
    }

    public String getType() {
        if (!TextUtils.isEmpty(type))
            return type;
        return getMessage();
    }
}
