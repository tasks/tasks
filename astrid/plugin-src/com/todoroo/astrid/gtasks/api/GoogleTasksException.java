package com.todoroo.astrid.gtasks.api;

import java.io.IOException;

public class GoogleTasksException extends IOException {
    private static final long serialVersionUID = -5585448790574862510L;

    public GoogleTasksException() {

    }

    public GoogleTasksException(String message) {
        super(message);
    }

    public GoogleTasksException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    public GoogleTasksException(Throwable cause) {
        super(cause.getMessage());
        initCause(cause);
    }
}
