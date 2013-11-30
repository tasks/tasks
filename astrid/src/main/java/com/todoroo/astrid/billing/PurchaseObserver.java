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
import com.todoroo.astrid.billing.BillingService.RequestPurchase;
import com.todoroo.astrid.billing.BillingService.RestoreTransactions;

import java.lang.reflect.Method;

public abstract class PurchaseObserver {
    protected static final String TAG = "purchase-observer"; //$NON-NLS-1$
    protected final Activity mActivity;
    private final Handler mHandler;
    private Method mStartIntentSender;
    private final Object[] mStartIntentSenderArgs = new Object[5];
    private static final Class<?>[] START_INTENT_SENDER_SIG = new Class[] {
        IntentSender.class, Intent.class, int.class, int.class, int.class
    };

    public PurchaseObserver(Activity activity, Handler handler) {
        mActivity = activity;
        mHandler = handler;
        initCompatibilityLayer();
    }

    public abstract void onBillingSupported(boolean supported, String type);

    public abstract void onPurchaseStateChange(PurchaseState purchaseState, String itemId);

    /**
     * This is called when we receive a response code from Market for a
     * RequestPurchase request that we made.  This is NOT used for any
     * purchase state changes.  All purchase state changes are received in
     * onPurchaseStateChange(PurchaseState, String, int, long).
     * This is used for reporting various errors, or if the user backed out
     * and didn't purchase the item.  The possible response codes are:
     *   RESULT_OK means that the order was sent successfully to the server.
     *       The onPurchaseStateChange() will be invoked later (with a
     *       purchase state of PURCHASED or CANCELED) when the order is
     *       charged or canceled.  This response code can also happen if an
     *       order for a Market-managed item was already sent to the server.
     *   RESULT_USER_CANCELED means that the user didn't buy the item.
     *   RESULT_SERVICE_UNAVAILABLE means that we couldn't connect to the
     *       Android Market server (for example if the data connection is down).
     *   RESULT_BILLING_UNAVAILABLE means that in-app billing is not
     *       supported yet.
     *   RESULT_ITEM_UNAVAILABLE means that the item this app offered for
     *       sale does not exist (or is not published) in the server-side
     *       catalog.
     *   RESULT_ERROR is used for any other errors (such as a server error).
     */
    public abstract void onRequestPurchaseResponse(RequestPurchase request,
            ResponseCode responseCode);

    /**
     * This is called when we receive a response code from Android Market for a
     * RestoreTransactions request that we made.  A response code of
     * RESULT_OK means that the request was successfully sent to the server.
     */
    public abstract void onRestoreTransactionsResponse(RestoreTransactions request,
            ResponseCode responseCode);

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
                onPurchaseStateChange(purchaseState, itemId);
            }
        });
    }
}
