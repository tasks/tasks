package com.todoroo.astrid.billing;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.billing.IMarketBillingService;
import com.todoroo.astrid.billing.BillingConstants.ResponseCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import static com.todoroo.astrid.billing.Security.Purchase;

@SuppressWarnings("nls")
public class BillingService extends Service implements ServiceConnection {
    private static final String TAG = "BillingService";

    private static IMarketBillingService marketBillingService;

    private static LinkedList<BillingRequest> pendingRequests = new LinkedList<>();

    private static HashMap<Long, BillingRequest> sentRequests = new HashMap<>();

    private AstridPurchaseObserver purchaseObserver;

    public boolean showDonateOption() {
        return purchaseObserver.isBillingSupported() && !purchaseObserver.userDonated();
    }

    abstract class BillingRequest {
        private final int mStartId;
        protected long mRequestId;

        public BillingRequest(int startId) {
            mStartId = startId;
        }

        public int getStartId() {
            return mStartId;
        }

        public boolean runRequest() {
            if (runIfConnected()) {
                return true;
            }

            if (bindToMarketBillingService()) {
                pendingRequests.add(this);
                return true;
            }
            return false;
        }

        /**
         * Try running the request directly if the service is already connected.
         *
         * @return true if the request ran successfully; false if the service
         * is not connected or there was an error when trying to use it
         */
        public boolean runIfConnected() {
            if (BillingConstants.DEBUG) {
                Log.d(TAG, getClass().getSimpleName());
            }
            if (marketBillingService != null) {
                try {
                    mRequestId = run();
                    if (BillingConstants.DEBUG) {
                        Log.d(TAG, "request id: " + mRequestId);
                    }
                    if (mRequestId >= 0) {
                        sentRequests.put(mRequestId, this);
                    }
                    return true;
                } catch (RemoteException e) {
                    onRemoteException(e);
                }
            }
            return false;
        }

        /**
         * Called when a remote exception occurs while trying to execute the
         * {@link #run()} method.  The derived class can override this to
         * execute exception-handling code.
         *
         * @param e the exception
         */
        protected void onRemoteException(RemoteException e) {
            Log.w(TAG, "remote billing service crashed");
            marketBillingService = null;
        }

        /**
         * The derived class must implement this method.
         *
         * @throws android.os.RemoteException
         */
        abstract protected long run() throws RemoteException;

        /**
         * This is called when Android Market sends a response code for this
         * request.
         *
         * @param responseCode the response code
         */
        protected void responseCodeReceived(ResponseCode responseCode) {
            //
        }

        protected Bundle makeRequestBundle(String method) {
            Bundle request = new Bundle();
            request.putString(BillingConstants.BILLING_REQUEST_METHOD, method);
            request.putInt(BillingConstants.BILLING_REQUEST_API_VERSION, 2);
            request.putString(BillingConstants.BILLING_REQUEST_PACKAGE_NAME, getPackageName());
            return request;
        }

        protected void logResponseCode(String method, Bundle response) {
            ResponseCode responseCode = ResponseCode.valueOf(
                    response.getInt(BillingConstants.BILLING_RESPONSE_RESPONSE_CODE));
            if (BillingConstants.DEBUG) {
                Log.e(TAG, method + " received " + responseCode.toString());
            }
        }
    }

    class CheckBillingSupported extends BillingRequest {
        public String mProductType = null;

        /**
         * Constructor
         * <p/>
         * Note: Support for subscriptions implies support for one-time purchases. However, the
         * opposite is not true.
         * <p/>
         * Developers may want to perform two checks if both one-time and subscription products are
         * available.
         *
         * @param itemType Either BillingConstants.ITEM_TYPE_INAPP or BillingConstants.ITEM_TYPE_SUBSCRIPTION, indicating
         *                 the type of item support is being checked for.
         */
        public CheckBillingSupported(String itemType) {
            super(-1);
            mProductType = itemType;
        }

        @Override
        protected long run() throws RemoteException {
            Bundle request = makeRequestBundle("CHECK_BILLING_SUPPORTED");
            if (mProductType != null) {
                request.putString(BillingConstants.BILLING_REQUEST_ITEM_TYPE, mProductType);
            }
            Bundle response = marketBillingService.sendBillingRequest(request);
            int responseCode = response.getInt(BillingConstants.BILLING_RESPONSE_RESPONSE_CODE);
            if (BillingConstants.DEBUG) {
                Log.i(TAG, "CheckBillingSupported response code: " +
                        ResponseCode.valueOf(responseCode));
            }
            boolean billingSupported = (responseCode == ResponseCode.RESULT_OK.ordinal());
            ResponseHandler.checkBillingSupportedResponse(billingSupported, mProductType);
            return BillingConstants.BILLING_RESPONSE_INVALID_REQUEST_ID;
        }
    }

    class RequestPurchase extends BillingRequest {
        public final String mProductId;
        public final String mDeveloperPayload;
        public final String mProductType;

        /**
         * Constructor
         *
         * @param itemId           The ID of the item to be purchased. Will be assumed to be a one-time
         *                         purchase.
         * @param itemType         Either BillingConstants.ITEM_TYPE_INAPP or BillingConstants.ITEM_TYPE_SUBSCRIPTION,
         *                         indicating the type of item type support is being checked for.
         * @param developerPayload Optional data.
         */
        public RequestPurchase(String itemId, String itemType, String developerPayload) {
            // This object is never created as a side effect of starting this
            // service so we pass -1 as the startId to indicate that we should
            // not stop this service after executing this request.
            super(-1);
            mProductId = itemId;
            mDeveloperPayload = developerPayload;
            mProductType = itemType;
        }

        @Override
        protected long run() throws RemoteException {
            Bundle request = makeRequestBundle("REQUEST_PURCHASE");
            request.putString(BillingConstants.BILLING_REQUEST_ITEM_ID, mProductId);
            request.putString(BillingConstants.BILLING_REQUEST_ITEM_TYPE, mProductType);
            // Note that the developer payload is optional.
            if (mDeveloperPayload != null) {
                request.putString(BillingConstants.BILLING_REQUEST_DEVELOPER_PAYLOAD, mDeveloperPayload);
            }
            Bundle response = marketBillingService.sendBillingRequest(request);
            PendingIntent pendingIntent
                    = response.getParcelable(BillingConstants.BILLING_RESPONSE_PURCHASE_INTENT);
            if (pendingIntent == null) {
                Log.e(TAG, "Error with requestPurchase");
                return BillingConstants.BILLING_RESPONSE_INVALID_REQUEST_ID;
            }

            Intent intent = new Intent();
            ResponseHandler.buyPageIntentResponse(pendingIntent, intent);
            return response.getLong(BillingConstants.BILLING_RESPONSE_REQUEST_ID,
                    BillingConstants.BILLING_RESPONSE_INVALID_REQUEST_ID);
        }

        @Override
        protected void responseCodeReceived(ResponseCode responseCode) {
            ResponseHandler.responseCodeReceived(this, responseCode);
        }
    }

    class ConfirmNotifications extends BillingRequest {
        final String[] mNotifyIds;

        public ConfirmNotifications(int startId, String[] notifyIds) {
            super(startId);
            mNotifyIds = notifyIds;
        }

        @Override
        protected long run() throws RemoteException {
            Bundle request = makeRequestBundle("CONFIRM_NOTIFICATIONS");
            request.putStringArray(BillingConstants.BILLING_REQUEST_NOTIFY_IDS, mNotifyIds);
            Bundle response = marketBillingService.sendBillingRequest(request);
            logResponseCode("confirmNotifications", response);
            return response.getLong(BillingConstants.BILLING_RESPONSE_REQUEST_ID,
                    BillingConstants.BILLING_RESPONSE_INVALID_REQUEST_ID);
        }
    }

    class GetPurchaseInformation extends BillingRequest {
        long mNonce;
        final String[] mNotifyIds;

        public GetPurchaseInformation(int startId, String[] notifyIds) {
            super(startId);
            mNotifyIds = notifyIds;
        }

        @Override
        protected long run() throws RemoteException {
            mNonce = Security.generateNonce();

            Bundle request = makeRequestBundle("GET_PURCHASE_INFORMATION");
            request.putLong(BillingConstants.BILLING_REQUEST_NONCE, mNonce);
            request.putStringArray(BillingConstants.BILLING_REQUEST_NOTIFY_IDS, mNotifyIds);
            Bundle response = marketBillingService.sendBillingRequest(request);
            logResponseCode("getPurchaseInformation", response);
            return response.getLong(BillingConstants.BILLING_RESPONSE_REQUEST_ID,
                    BillingConstants.BILLING_RESPONSE_INVALID_REQUEST_ID);
        }
    }

    class RestoreTransactions extends BillingRequest {
        long mNonce;

        public RestoreTransactions() {
            // This object is never created as a side effect of starting this
            // service so we pass -1 as the startId to indicate that we should
            // not stop this service after executing this request.
            super(-1);
        }

        @Override
        protected long run() throws RemoteException {
            mNonce = Security.generateNonce();

            Bundle request = makeRequestBundle("RESTORE_TRANSACTIONS");
            request.putLong(BillingConstants.BILLING_REQUEST_NONCE, mNonce);
            Bundle response = marketBillingService.sendBillingRequest(request);
            logResponseCode("restoreTransactions", response);
            return response.getLong(BillingConstants.BILLING_RESPONSE_REQUEST_ID,
                    BillingConstants.BILLING_RESPONSE_INVALID_REQUEST_ID);
        }

        @Override
        protected void responseCodeReceived(ResponseCode responseCode) {
            ResponseHandler.responseCodeReceived(this, responseCode);
        }
    }

    public void setActivity(Activity activity) {
        attachBaseContext(activity);
        purchaseObserver = new AstridPurchaseObserver(activity, this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // binding not supported for this service
    }

    @Override
    public void onStart(Intent intent, int startId) {
        handleCommand(intent, startId);
    }

    /**
     * The {@link BillingReceiver} sends messages to this service using intents.
     * Each intent has an action and some extra arguments specific to that action.
     *
     * @param intent  the intent containing one of the supported actions
     * @param startId an identifier for the invocation instance of this service
     */
    private void handleCommand(Intent intent, int startId) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        Log.d(TAG, "handleCommand(" + action + ")");
        switch (action) {
            case BillingConstants.ACTION_CONFIRM_NOTIFICATION:
                String[] notifyIds = intent.getStringArrayExtra(BillingConstants.NOTIFICATION_ID);
                confirmNotifications(startId, notifyIds);
                break;
            case BillingConstants.ACTION_GET_PURCHASE_INFORMATION:
                String notifyId = intent.getStringExtra(BillingConstants.NOTIFICATION_ID);
                getPurchaseInformation(startId, new String[]{notifyId});
                break;
            case BillingConstants.ACTION_PURCHASE_STATE_CHANGED:
                String signedData = intent.getStringExtra(BillingConstants.INAPP_SIGNED_DATA);
                purchaseStateChanged(startId, signedData);
                break;
            case BillingConstants.ACTION_RESPONSE_CODE:
                long requestId = intent.getLongExtra(BillingConstants.INAPP_REQUEST_ID, -1);
                int responseCodeIndex = intent.getIntExtra(BillingConstants.INAPP_RESPONSE_CODE,
                        ResponseCode.RESULT_ERROR.ordinal());
                ResponseCode responseCode = ResponseCode.valueOf(responseCodeIndex);
                checkResponseCode(requestId, responseCode);
                break;
        }
    }

    private boolean bindToMarketBillingService() {
        Log.d(TAG, "bindToMarketBillingService()");
        try {
            boolean bindResult = bindService(
                    new Intent(BillingConstants.MARKET_BILLING_SERVICE_ACTION),
                    this,
                    Context.BIND_AUTO_CREATE);

            if (bindResult) {
                return true;
            } else {
                Log.e(TAG, "Could not bind to service.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e);
        }
        return false;
    }

    public boolean checkBillingSupported() {
        return new CheckBillingSupported(BillingConstants.ITEM_TYPE_INAPP).runRequest();
    }

    /**
     * Requests that the given item be offered to the user for purchase. When
     * the purchase succeeds (or is canceled) the {@link BillingReceiver}
     * receives an intent with the action {@link BillingConstants#ACTION_NOTIFY}.
     * Returns false if there was an error trying to connect to Android Market.
     *
     * @param productId        an identifier for the item being offered for purchase
     * @param itemType         Either BillingConstants.ITEM_TYPE_INAPP or BillingConstants.ITEM_TYPE_SUBSCRIPTION, indicating
     *                         the type of item type support is being checked for.
     * @param developerPayload a payload that is associated with a given
     *                         purchase, if null, no payload is sent
     * @return false if there was an error connecting to Android Market
     */
    public boolean requestPurchase(String productId, String itemType, String developerPayload) {
        return new RequestPurchase(productId, itemType, developerPayload).runRequest();
    }

    /**
     * Requests transaction information for all managed items. Call this only when the
     * application is first installed or after a database wipe. Do NOT call this
     * every time the application starts up.
     *
     * @return false if there was an error connecting to Android Market
     */
    public boolean restoreTransactions() {
        return new RestoreTransactions().runRequest();
    }

    /**
     * Confirms receipt of a purchase state change. Each {@code notifyId} is
     * an opaque identifier that came from the server. This method sends those
     * identifiers back to the MarketBillingService, which ACKs them to the
     * server. Returns false if there was an error trying to connect to the
     * MarketBillingService.
     *
     * @param startId   an identifier for the invocation instance of this service
     * @param notifyIds a list of opaque identifiers associated with purchase
     *                  state changes.
     * @return false if there was an error connecting to Market
     */
    private boolean confirmNotifications(int startId, String[] notifyIds) {
        return new ConfirmNotifications(startId, notifyIds).runRequest();
    }

    /**
     * Gets the purchase information. This message includes a list of
     * notification IDs sent to us by Android Market, which we include in
     * our request. The server responds with the purchase information,
     * encoded as a JSON string, and sends that to the {@link BillingReceiver}
     * in an intent with the action {@link BillingConstants#ACTION_PURCHASE_STATE_CHANGED}.
     * Returns false if there was an error trying to connect to the MarketBillingService.
     *
     * @param startId   an identifier for the invocation instance of this service
     * @param notifyIds a list of opaque identifiers associated with purchase
     *                  state changes
     * @return false if there was an error connecting to Android Market
     */
    private boolean getPurchaseInformation(int startId, String[] notifyIds) {
        return new GetPurchaseInformation(startId, notifyIds).runRequest();
    }

    /**
     * Verifies that the data was signed with the given signature, and calls
     * ResponseHandler.purchaseResponse(android.content.Context, PurchaseState, String, String, long)
     * for each verified purchase.
     *
     * @param startId    an identifier for the invocation instance of this service
     * @param signedData the signed JSON string (signed, not encrypted)
     */
    private void purchaseStateChanged(int startId, String signedData) {
        ArrayList<Purchase> purchases;
        purchases = Security.parse(signedData);
        ArrayList<String> notifyList = new ArrayList<>();
        for (Purchase vp : purchases) {
            if (vp.notificationId != null) {
                notifyList.add(vp.notificationId);
            }
            ResponseHandler.purchaseResponse(vp.purchaseState, vp.productId);
        }
        if (!notifyList.isEmpty()) {
            String[] notifyIds = notifyList.toArray(new String[notifyList.size()]);
            confirmNotifications(startId, notifyIds);
        }
    }

    private void checkResponseCode(long requestId, ResponseCode responseCode) {
        BillingRequest request = sentRequests.get(requestId);
        if (request != null) {
            Log.d(TAG, request.getClass().getSimpleName() + ": " + responseCode);
            request.responseCodeReceived(responseCode);
        }
        sentRequests.remove(requestId);
    }

    /**
     * Runs any pending requests that are waiting for a connection to the
     * service to be established.  This runs in the main UI thread.
     */
    private void runPendingRequests() {
        int maxStartId = -1;
        BillingRequest request;
        while ((request = pendingRequests.peek()) != null) {
            if (request.runIfConnected()) {
                // Remove the request
                pendingRequests.remove();

                // Remember the largest startId, which is the most recent
                // request to start this service.
                if (maxStartId < request.getStartId()) {
                    maxStartId = request.getStartId();
                }
            } else {
                // The service crashed, so restart it. Note that this leaves
                // the current request on the queue.
                bindToMarketBillingService();
                return;
            }
        }

        // If we get here then all the requests ran successfully.  If maxStartId
        // is not -1, then one of the requests started the service, so we can
        // stop it now.
        if (maxStartId >= 0) {
            if (BillingConstants.DEBUG) {
                Log.i(TAG, "stopping service, startId: " + maxStartId);
            }
            stopSelf(maxStartId);
        }
    }

    /**
     * This is called when we are connected to the MarketBillingService.
     * This runs in the main UI thread.
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "Billing service connected");
        marketBillingService = IMarketBillingService.Stub.asInterface(service);
        runPendingRequests();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.w(TAG, "Billing service disconnected");
        marketBillingService = null;
    }

    public void unbind() {
        try {
            unbindService(this);
        } catch (IllegalArgumentException e) {
            // This might happen if the service was disconnected
        }
    }

    public void onStart() {
        ResponseHandler.register(purchaseObserver);
    }

    public void onStop() {
        ResponseHandler.unregister();
    }
}
