/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.IOException;

import org.json.JSONObject;


/**
 * Exception that wraps an exception encountered during API invocation or
 * processing.
 *
 * @author timsu
 *
 */
public class ActFmServiceException extends IOException {

    private static final long serialVersionUID = -2803924196075428257L;

    public JSONObject result;

    public ActFmServiceException(String detailMessage, JSONObject result) {
        super(detailMessage);
        this.result = result;
    }

    public ActFmServiceException(Throwable throwable, JSONObject result) {
        super(throwable.getMessage());
        initCause(throwable);
        this.result = result;
    }

    public ActFmServiceException(JSONObject result) {
        super();
        this.result = result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getMessage(); //$NON-NLS-1$
    }

}
