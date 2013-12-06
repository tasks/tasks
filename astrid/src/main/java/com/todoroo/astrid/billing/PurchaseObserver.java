// Copyright 2010 Google Inc. All Rights Reserved.

package com.todoroo.astrid.billing;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Handler;
import android.util.Log;

import com.todoroo.astrid.billing.BillingConstants.PurchaseState;
import com.todoroo.astrid.billing.BillingConstants.ResponseCode;

import org.tasks.billing.PurchaseHandler;

import java.lang.reflect.Method;

public class PurchaseObserver {
    protected static final String TAG = "purchase-observer"; //$NON-NLS-1$
    protected final Activity mActivity;
    private PurchaseHandler purchaseHandler;
    private final Handler mHandler = new Handler();
    private Method mStartIntentSender;
    private final Object[] mStartIntentSenderArgs = new Object[5];
    private static final Class<?>[] START_INTENT_SENDER_SIG = new Class[] {
        IntentSender.class, Intent.class, int.class, int.class, int.class
    };

    public PurchaseObserver(Activity activity, PurchaseHandler purchaseHandler) {
        mActivity = activity;
        this.purchaseHandler = purchaseHandler;
        initCompatibilityLayer();
    }

    public void onBillingSupported(boolean supported, String type) {
        purchaseHandler.onBillingSupported(supported, type);
    }

    public void onRestoreTransactionsResponse(ResponseCode responseCode) {
        purchaseHandler.onRestoreTransactionsResponse(responseCode);
    }

    private void initCompatibilityLayer() {
        try {
            mStartIntentSender = mActivity.getClass().getMethod("startIntentSender", //$NON-NLS-1$
                    START_INTENT_SENDER_SIG);
        } catch (SecurityException | NoSuchMethodException e) {
            mStartIntentSender = null;
        }
    }

    void startBuyPageActivity(PendingIntent pendingIntent, Intent intent) {
        try {
            mStartIntentSenderArgs[0] = pendingIntent.getIntentSender();
            mStartIntentSenderArgs[1] = intent;
            mStartIntentSenderArgs[2] = 0;
            mStartIntentSenderArgs[3] = 0;
            mStartIntentSenderArgs[4] = 0;
            mStartIntentSender.invoke(mActivity, mStartIntentSenderArgs);
        } catch (Exception e) {
            Log.e(TAG, "error starting activity", e); //$NON-NLS-1$
        }
    }

    void postPurchaseStateChange(final PurchaseState purchaseState, final String itemId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                purchaseHandler.onPurchaseStateChange(purchaseState, itemId);
            }
        });
    }
}
