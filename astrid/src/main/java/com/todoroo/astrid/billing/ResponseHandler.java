package com.todoroo.astrid.billing;

import android.app.PendingIntent;
import android.content.Intent;

import com.todoroo.astrid.billing.BillingConstants.PurchaseState;
import com.todoroo.astrid.billing.BillingConstants.ResponseCode;
import com.todoroo.astrid.billing.BillingService.RequestPurchase;
import com.todoroo.astrid.billing.BillingService.RestoreTransactions;

public class ResponseHandler {

    private static PurchaseObserver sPurchaseObserver;

    public static synchronized void register(PurchaseObserver observer) {
        sPurchaseObserver = observer;
    }

    public static synchronized void unregister() {
        sPurchaseObserver = null;
    }

    public static void checkBillingSupportedResponse(boolean supported, String type) {
        if (sPurchaseObserver != null) {
            sPurchaseObserver.onBillingSupported(supported, type);
        }
    }

    public static void buyPageIntentResponse(PendingIntent pendingIntent, Intent intent) {
        if (sPurchaseObserver != null) {
            sPurchaseObserver.startBuyPageActivity(pendingIntent, intent);
        }
    }

    public static void purchaseResponse(final PurchaseState purchaseState, final String productId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // This needs to be synchronized because the UI thread can change the
                // value of sPurchaseObserver.
                synchronized (ResponseHandler.class) {
                    if (sPurchaseObserver != null) {
                        sPurchaseObserver.postPurchaseStateChange(purchaseState, productId);
                    }
                }
            }
        }).start();
    }

    public static void responseCodeReceived(RequestPurchase request, ResponseCode responseCode) {
        if (sPurchaseObserver != null) {
            sPurchaseObserver.onRequestPurchaseResponse(request, responseCode);
        }
    }

    public static void responseCodeReceived(RestoreTransactions request, ResponseCode responseCode) {
        if (sPurchaseObserver != null) {
            sPurchaseObserver.onRestoreTransactionsResponse(request, responseCode);
        }
    }
}
