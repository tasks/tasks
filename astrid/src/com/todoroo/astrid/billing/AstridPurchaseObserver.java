package com.todoroo.astrid.billing;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.billing.BillingConstants.PurchaseState;
import com.todoroo.astrid.billing.BillingConstants.ResponseCode;
import com.todoroo.astrid.billing.BillingService.RequestPurchase;
import com.todoroo.astrid.billing.BillingService.RestoreTransactions;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public abstract class AstridPurchaseObserver extends PurchaseObserver {

    @Autowired
    private ActFmSyncService actFmSyncService;

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    /**
     * A {@link PurchaseObserver} is used to get callbacks when Android Market sends
     * messages to this application so that we can update the UI.
     */
    public AstridPurchaseObserver(Activity activity, Handler handler) {
        super(activity, handler);
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public void onBillingSupported(boolean supported, String type) {
        if (Constants.DEBUG) {
            Log.i(TAG, "supported: " + supported);
        }
        if (type != null && type.equals(BillingConstants.ITEM_TYPE_SUBSCRIPTION)) {
            if (supported) {
                billingSupportedCallback();
            } else {
                billingNotSupportedCallback();
            }
        } else {
            subscriptionsNotSupportedCallback();
        }
    }

    protected abstract void billingSupportedCallback();

    protected abstract void billingNotSupportedCallback();

    protected abstract void subscriptionsNotSupportedCallback();

    @Override
    public void onPurchaseStateChange(PurchaseState purchaseState, final String itemId,
            int quantity, long purchaseTime, String developerPayload, final String purchaseToken) {
        if (Constants.DEBUG) {
            Log.i(TAG, "onPurchaseStateChange() itemId: " + itemId + " " + purchaseState);
        }

        Preferences.setString(BillingConstants.PREF_PRODUCT_ID, itemId);
        Preferences.setString(BillingConstants.PREF_PURCHASE_TOKEN, purchaseToken);

        if (purchaseState == PurchaseState.PURCHASED) {
            new Thread() {
                @Override
                public void run() {
                    Preferences.setBoolean(ActFmPreferenceService.PREF_LOCAL_PREMIUM, true);
                    if (actFmPreferenceService.isLoggedIn()) {
                        actFmSyncService.updateUserSubscriptionStatus(new Runnable() {
                            @Override
                            public void run() {
                                Preferences.setBoolean(ActFmPreferenceService.PREF_PREMIUM, true);
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mActivity, R.string.premium_success, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }, new Runnable() {
                            @Override
                            public void run() {
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mActivity, R.string.premium_success_with_server_error, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        });
                    }
                }
            }.start();
        } else if (purchaseState == PurchaseState.REFUNDED || purchaseState == PurchaseState.EXPIRED) {
            new Thread() {
                @Override
                public void run() {
                    Preferences.setBoolean(ActFmPreferenceService.PREF_LOCAL_PREMIUM, false);
                    if (actFmPreferenceService.isLoggedIn())
                        actFmSyncService.updateUserSubscriptionStatus(null, null);
                }
            }.start();
        }
    }

    @Override
    public void onRequestPurchaseResponse(RequestPurchase request,
            ResponseCode responseCode) {
        if (Constants.DEBUG) {
            Log.d(TAG, request.mProductId + ": " + responseCode);
        }
        if (responseCode == ResponseCode.RESULT_OK) {
            if (Constants.DEBUG) {
                Log.i(TAG, "purchase was successfully sent to server");
            }
        } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
            if (Constants.DEBUG) {
                Log.i(TAG, "user canceled purchase");
            }
        } else {
            if (Constants.DEBUG) {
                Log.i(TAG, "purchase failed");
            }
        }
    }

    @Override
    public void onRestoreTransactionsResponse(RestoreTransactions request,
            ResponseCode responseCode) {
        if (responseCode == ResponseCode.RESULT_OK) {
            if (Constants.DEBUG) {
                Log.d(TAG, "completed RestoreTransactions request");
            }
            // Update the shared preferences so that we don't perform
            // a RestoreTransactions again.
            Preferences.setBoolean(BillingConstants.PREF_TRANSACTIONS_INITIALIZED, true);
        } else {
            if (Constants.DEBUG) {
                Log.d(TAG, "RestoreTransactions error: " + responseCode);
            }
        }
    }

}
