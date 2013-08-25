/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;

import java.io.IOException;

public class HttpErrorException extends IOException {

    private static final long serialVersionUID = 5373340422464657279L;

    public HttpErrorException(int code, String message) {
        super(String.format("%d %s", code, message)); //$NON-NLS-1$
    }

}
