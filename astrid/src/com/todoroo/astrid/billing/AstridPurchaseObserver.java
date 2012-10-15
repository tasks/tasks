package com.todoroo.astrid.billing;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.billing.BillingConstants.PurchaseState;
import com.todoroo.astrid.billing.BillingConstants.ResponseCode;
import com.todoroo.astrid.billing.BillingService.RequestPurchase;
import com.todoroo.astrid.billing.BillingService.RestoreTransactions;

@SuppressWarnings("nls")
public abstract class AstridPurchaseObserver extends PurchaseObserver {

    @Autowired
    private ActFmSyncService actFmSyncService;

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    public interface RestoreTransactionsListener {
        public void restoreTransactionsResponse(ResponseCode responseCode);
    }

    private RestoreTransactionsListener restoreTransactionsListener;

    /**
     * A {@link PurchaseObserver} is used to get callbacks when Android Market sends
     * messages to this application so that we can update the UI.
     */
    public AstridPurchaseObserver(Activity activity, Handler handler) {
        super(activity, handler);
        DependencyInjectionService.getInstance().inject(this);
    }

    public void setRestoreTransactionsListener(RestoreTransactionsListener listener) {
        this.restoreTransactionsListener = listener;
    }

    @Override
    public void onBillingSupported(boolean supported, String type) {
        if (BillingConstants.DEBUG) {
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
        if (BillingConstants.DEBUG) {
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
                            public void run() { // On Success
                                Preferences.setBoolean(ActFmPreferenceService.PREF_PREMIUM, true);
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DialogUtilities.okDialog(mActivity, mActivity.getString(R.string.DLG_information_title),
                                                0, mActivity.getString(R.string.premium_success), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        mActivity.finish();
                                                    }
                                                });
                                    }
                                });
                            }
                        }, new Runnable() { // On Recoverable error
                            @Override
                            public void run() {
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DialogUtilities.okDialog(mActivity, mActivity.getString(R.string.DLG_information_title),
                                                0, mActivity.getString(R.string.premium_success_with_server_error), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        mActivity.finish();
                                                    }
                                                });
                                    }
                                });
                            }
                        }, new Runnable() { // On invalid token
                            @Override
                            public void run() {
                                DialogUtilities.okDialog(mActivity, mActivity.getString(R.string.DLG_information_title),
                                        0, mActivity.getString(R.string.premium_verification_error), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mActivity.finish();
                                    }
                                });
                            }
                        });
                    } else {
                        Preferences.setBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, true);
                    }
                }
            }.start();
        } else if (purchaseState == PurchaseState.REFUNDED) {
            new Thread() {
                @Override
                public void run() {
                    Preferences.setBoolean(ActFmPreferenceService.PREF_LOCAL_PREMIUM, false);
                    if (actFmPreferenceService.isLoggedIn())
                        actFmSyncService.updateUserSubscriptionStatus(null, null, null);
                }
            }.start();
        }
    }

    @Override
    public void onRequestPurchaseResponse(RequestPurchase request,
            ResponseCode responseCode) {
        if (BillingConstants.DEBUG) {
            Log.d(TAG, request.mProductId + ": " + responseCode);
        }
        if (responseCode == ResponseCode.RESULT_OK) {
            if (BillingConstants.DEBUG) {
                Log.i(TAG, "purchase was successfully sent to server");
            }
        } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
            if (BillingConstants.DEBUG) {
                Log.i(TAG, "user canceled purchase");
            }
        } else {
            if (BillingConstants.DEBUG) {
                Log.i(TAG, "purchase failed");
            }
        }
    }

    @Override
    public void onRestoreTransactionsResponse(RestoreTransactions request,
            ResponseCode responseCode) {
        if (responseCode == ResponseCode.RESULT_OK) {
            if (BillingConstants.DEBUG) {
                Log.d(TAG, "completed RestoreTransactions request");
            }
            // Update the shared preferences so that we don't perform
            // a RestoreTransactions again.
            Preferences.setBoolean(BillingConstants.PREF_TRANSACTIONS_INITIALIZED, true);
        } else {
            if (BillingConstants.DEBUG) {
                Log.d(TAG, "RestoreTransactions error: " + responseCode);
            }
        }
        if (restoreTransactionsListener != null)
            restoreTransactionsListener.restoreTransactionsResponse(responseCode);
    }

}
