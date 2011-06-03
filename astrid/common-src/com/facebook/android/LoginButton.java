/*
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.facebook.android.Facebook.DialogListener;

public class LoginButton extends Button {

    public static final int REQUEST_CODE_FACEBOOK = 21421;

    private Facebook mFb;
    private AuthListener mListener;
    private String[] mPermissions;
    private Activity mActivity;

    public LoginButton(Context context) {
        super(context);
    }

    public LoginButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LoginButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(final Activity activity, final Facebook fb, AuthListener listener) {
    	init(activity, fb, listener, new String[] {});
    }

    public void init(final Activity activity, final Facebook fb, AuthListener listener,
                     final String[] permissions) {
        mActivity = activity;
        mFb = fb;
        mPermissions = permissions;
        mListener = listener;

        setOnClickListener(new ButtonOnClickListener());
    }

    private final class ButtonOnClickListener implements OnClickListener {

        public void onClick(View arg0) {
            mFb.authorize(mActivity, mPermissions, REQUEST_CODE_FACEBOOK,
                    new LoginDialogListener());
        }
    }

    private final class LoginDialogListener implements DialogListener {
        public void onComplete(Bundle values) {
            mListener.onFBAuthSucceed();
        }

        public void onFacebookError(FacebookError error) {
            mListener.onFBAuthFail(error.getMessage());
        }

        public void onError(DialogError error) {
            mListener.onFBAuthFail(error.getMessage());
        }

        public void onCancel() {
            mListener.onFBAuthCancel();
        }
    }

}
