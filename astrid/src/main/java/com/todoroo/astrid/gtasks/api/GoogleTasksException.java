/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import java.io.IOException;

public class GoogleTasksException extends IOException {
    public GoogleTasksException(String message) {
        super(message);
    }
}
