package com.todoroo.astrid.billing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.todoroo.astrid.billing.BillingConstants.ResponseCode;

public class BillingReceiver extends BroadcastReceiver {
    private static final String TAG = "billing-receiver"; //$NON-NLS-1$

    /**
     * This is the entry point for all asynchronous messages sent from Android Market to
     * the application. This method forwards the messages on to the
     * {@link BillingService}, which handles the communication back to Android Market.
     * The {@link BillingService} also reports state changes back to the application through
     * the {@link ResponseHandler}.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BillingConstants.ACTION_PURCHASE_STATE_CHANGED.equals(action)) {
            String signedData = intent.getStringExtra(BillingConstants.INAPP_SIGNED_DATA);
            String signature = intent.getStringExtra(BillingConstants.INAPP_SIGNATURE);
            purchaseStateChanged(context, signedData, signature);
        } else if (BillingConstants.ACTION_NOTIFY.equals(action)) {
            String notifyId = intent.getStringExtra(BillingConstants.NOTIFICATION_ID);
            if (BillingConstants.DEBUG) {
                Log.i(TAG, "notifyId: " + notifyId); //$NON-NLS-1$
            }
            notify(context, notifyId);
        } else if (BillingConstants.ACTION_RESPONSE_CODE.equals(action)) {
            long requestId = intent.getLongExtra(BillingConstants.INAPP_REQUEST_ID, -1);
            int responseCodeIndex = intent.getIntExtra(BillingConstants.INAPP_RESPONSE_CODE,
                    ResponseCode.RESULT_ERROR.ordinal());
            checkResponseCode(context, requestId, responseCodeIndex);
        } else {
            Log.w(TAG, "unexpected action: " + action); //$NON-NLS-1$
        }
    }

    /**
     * This is called when Android Market sends information about a purchase state
     * change. The signedData parameter is a plaintext JSON string that is
     * signed by the server with the developer's private key. The signature
     * for the signed data is passed in the signature parameter.
     * @param context the context
     * @param signedData the (unencrypted) JSON string
     * @param signature the signature for the signedData
     */
    private void purchaseStateChanged(Context context, String signedData, String signature) {
        Intent intent = new Intent(BillingConstants.ACTION_PURCHASE_STATE_CHANGED);
        intent.setClass(context, BillingService.class);
        intent.putExtra(BillingConstants.INAPP_SIGNED_DATA, signedData);
        intent.putExtra(BillingConstants.INAPP_SIGNATURE, signature);
        context.startService(intent);
    }

    /**
     * This is called when Android Market sends a "notify" message  indicating that transaction
     * information is available. The request includes a nonce (random number used once) that
     * we generate and Android Market signs and sends back to us with the purchase state and
     * other transaction details. This BroadcastReceiver cannot bind to the
     * MarketBillingService directly so it starts the {@link BillingService}, which does the
     * actual work of sending the message.
     *
     * @param context the context
     * @param notifyId the notification ID
     */
    private void notify(Context context, String notifyId) {
        Intent intent = new Intent(BillingConstants.ACTION_GET_PURCHASE_INFORMATION);
        intent.setClass(context, BillingService.class);
        intent.putExtra(BillingConstants.NOTIFICATION_ID, notifyId);
        context.startService(intent);
    }

    /**
     * This is called when Android Market sends a server response code. The BillingService can
     * then report the status of the response if desired.
     *
     * @param context the context
     * @param requestId the request ID that corresponds to a previous request
     * @param responseCodeIndex the ResponseCode ordinal value for the request
     */
    private void checkResponseCode(Context context, long requestId, int responseCodeIndex) {
        Intent intent = new Intent(BillingConstants.ACTION_RESPONSE_CODE);
        intent.setClass(context, BillingService.class);
        intent.putExtra(BillingConstants.INAPP_REQUEST_ID, requestId);
        intent.putExtra(BillingConstants.INAPP_RESPONSE_CODE, responseCodeIndex);
        context.startService(intent);
    }
}
