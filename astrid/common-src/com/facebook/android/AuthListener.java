/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.facebook.android;


public interface AuthListener {

    public void onFBAuthSucceed();

    public void onFBAuthFail(String error);

    public void onFBAuthCancel();

}
