/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev.api;

/**
 * Exception that is thrown when an authentication exception occurs and users
 * need to sign in
 *
 * @author timsu
 *
 */
public class ApiAuthenticationException extends ApiServiceException {

    private static final long serialVersionUID = 1696103465107607150L;

    public ApiAuthenticationException(String detailMessage) {
        super(detailMessage);
    }

}
