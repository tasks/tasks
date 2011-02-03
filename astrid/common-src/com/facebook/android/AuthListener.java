package com.facebook.android;


public interface AuthListener {

    public void onFBAuthSucceed();

    public void onFBAuthFail(String error);

    public void onFBAuthCancel();

}
