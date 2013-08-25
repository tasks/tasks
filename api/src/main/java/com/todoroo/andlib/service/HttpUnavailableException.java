/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;

import java.io.IOException;

/**
 * Exception displayed when a 500 error is received on an HTTP invocation
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HttpUnavailableException extends IOException {

    private static final long serialVersionUID = 5373340422464657279L;

    public HttpUnavailableException() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public String getMessage() {
        return "Sorry, our servers are experiencing some issues. Please try again later!"; //$NON-NLS-1$ // FIXME
    }

}
