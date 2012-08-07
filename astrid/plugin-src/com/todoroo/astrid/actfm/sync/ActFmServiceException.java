/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.IOException;


/**
 * Exception that wraps an exception encountered during API invocation or
 * processing.
 *
 * @author timsu
 *
 */
public class ActFmServiceException extends IOException {

    private static final long serialVersionUID = -2803924196075428257L;

    public ActFmServiceException(String detailMessage) {
        super(detailMessage);
    }

    public ActFmServiceException(Throwable throwable) {
        super(throwable.getMessage());
        initCause(throwable);
    }

    public ActFmServiceException() {
        super();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getMessage(); //$NON-NLS-1$
    }

}
