package com.todoroo.astrid.billing;

import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.billing.BillingConstants.ResponseCode;
import com.todoroo.astrid.service.ThemeService;

public class BillingActivity extends SherlockFragmentActivity implements AstridPurchaseObserver.RestoreTransactionsListener {

    private static final int DIALOG_CANNOT_CONNECT_ID = 1;
    private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;
    private static final int DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID = 3;

    private Handler handler;
    private BillingService billingService;
    private AstridPurchaseObserver purchaseObserver;
    private Button buyMonth;
    private Button buyYear;
    private TextView restorePurchases;

    private ProgressDialog restoreTransactionsDialog;

    @Autowired private ActFmPreferenceService actFmPreferenceService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
        DependencyInjectionService.getInstance().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.billing_activity);

        setupActionBar();

        setupButtons();

        setupText();

        handler = new Handler();
        billingService = new BillingService();
        billingService.setContext(this);
        purchaseObserver = new AstridPurchaseObserver(this, handler) {
            @Override
            protected void billingSupportedCallback() {
                restoreTransactions(false);
                buyMonth.setEnabled(true);
                buyYear.setEnabled(true);
                restorePurchases.setEnabled(true);
            }

            @Override
            protected void billingNotSupportedCallback() {
                showDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
            }

            @Override
            protected void subscriptionsNotSupportedCallback() {
                showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
            }
        };
        purchaseObserver.setRestoreTransactionsListener(this);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.header_title_view);
        ((TextView) actionBar.getCustomView().findViewById(R.id.title)).setText(R.string.premium_billing_title);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ResponseHandler.register(purchaseObserver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ResponseHandler.unregister(purchaseObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        billingService.unbind();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!actFmPreferenceService.isLoggedIn()) {
            // Prompt to log in, but this shouldn't happen anyways since we hide the entry path to this screen when not logged in
            DialogUtilities.okDialog(this, getString(R.string.premium_login_prompt),
                    new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        } else if (!billingService.checkBillingSupported(BillingConstants.ITEM_TYPE_SUBSCRIPTION)) {
            showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
        } else if (ActFmPreferenceService.isPremiumUser()) {
            DialogUtilities.okDialog(this, getString(R.string.premium_already_subscribed), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }
    }

    private void setupButtons() {
        buyMonth = (Button) findViewById(R.id.premium_buy_month);
        buyYear = (Button) findViewById(R.id.premium_buy_year);
        restorePurchases = (TextView) findViewById(R.id.check_for_purchases);

        buyMonth.setEnabled(false);
        buyYear.setEnabled(false);
        restorePurchases.setEnabled(false);

        if (!Preferences.getBoolean(BillingConstants.PREF_TRANSACTIONS_INITIALIZED, false))
            restorePurchases.setVisibility(View.GONE);

        buyMonth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!billingService.requestPurchase(BillingConstants.PRODUCT_ID_MONTHLY,
                        BillingConstants.ITEM_TYPE_SUBSCRIPTION, null)) {
                    showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
                }
            }
        });

        buyYear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!billingService.requestPurchase(BillingConstants.PRODUCT_ID_YEARLY,
                        BillingConstants.ITEM_TYPE_SUBSCRIPTION, null)) {
                    showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
                }
            }
        });

        restorePurchases.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                restoreTransactions(true);
            }
        });
    }

    @SuppressWarnings("nls")
    private void setupText() {
        int[] bullets = new int[] { R.string.premium_description_1, /* R.string.premium_description_2,*/ R.string.premium_description_3,
                R.string.premium_description_4, R.string.premium_description_5, R.string.premium_description_6
        };

        StringBuilder builder = new StringBuilder("<html><style type=\"text/css\">li { padding-bottom: 13px } </style><body><ul>");

        for (int i = 0; i < bullets.length; i++) {
            String curr = getString(bullets[i]);
            if (curr.contains("\n"))
                curr = curr.replace("\n", "<br>");
            builder.append("<li><font style='color=#404040; font-size: 18px'>").append(curr);

            builder.append("</font></li>\n");
        }

        builder.append("</ul></body></html>");

        WebView list = (WebView) findViewById(R.id.premium_bullets);
        list.loadDataWithBaseURL("file:///android_asset/", builder.toString(), "text/html", "utf-8", null);
        list.setBackgroundColor(0);

        View speechBubbleBackground = findViewById(R.id.speech_bubble_container);
        speechBubbleBackground.setBackgroundColor(0);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        ImageView icon = (ImageView) findViewById(R.id.astridIcon);

        int dim = (int) (80 * metrics.density);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dim, dim));
        icon.setScaleType(ScaleType.FIT_CENTER);

        TextView speechBubble = (TextView) findViewById(R.id.reminder_message);

        // Construct speech bubble text
        String html = String.format("%s <font color=\"#%s\">%s</font>",
                getString(R.string.premium_speech_bubble_1),
                Integer.toHexString(getResources().getColor(R.color.red_theme_color) - 0xff000000),
                getString(R.string.premium_speech_bubble_2));
        Spanned spanned = Html.fromHtml(html);
        speechBubble.setText(spanned);
        speechBubble.setTextSize(17);
    }

    /**
     * Replaces the language and/or country of the device into the given string.
     * The pattern "%lang%" will be replaced by the device's language code and
     * the pattern "%region%" will be replaced with the device's country code.
     *
     * @param str the string to replace the language/country within
     * @return a string containing the local language and region codes
     */
    @SuppressWarnings("nls")
    private String replaceLanguageAndRegion(String str) {
        // Substitute language and or region if present in string
        if (str.contains("%lang%") || str.contains("%region%")) {
            Locale locale = Locale.getDefault();
            str = str.replace("%lang%", locale.getLanguage().toLowerCase());
            str = str.replace("%region%", locale.getCountry().toLowerCase());
        }
        return str;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_CANNOT_CONNECT_ID:
            return createDialog(R.string.cannot_connect_title,
                    R.string.cannot_connect_message);
        case DIALOG_BILLING_NOT_SUPPORTED_ID:
            return createDialog(R.string.billing_not_supported_title,
                    R.string.billing_not_supported_message);
            case DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID:
                return createDialog(R.string.subscriptions_not_supported_title,
                        R.string.subscriptions_not_supported_message);
        default:
            return null;
        }
    }

    @SuppressWarnings("nls")
    private Dialog createDialog(int titleId, int messageId) {
        String helpUrl = "http://market.android.com/support/bin/answer.py?answer=1050566&amp;hl=%lang%&amp;dl=%region%";
        helpUrl = replaceLanguageAndRegion(helpUrl);
        if (BillingConstants.DEBUG) {
            Log.i("billing-activity-url", helpUrl); //$NON-NLS-1$
        }
        final Uri helpUri = Uri.parse(helpUrl);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titleId)
            .setIcon(android.R.drawable.stat_sys_warning)
            .setMessage(messageId)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .setNegativeButton(R.string.subscriptions_learn_more, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, helpUri);
                    startActivity(intent);
                }
            });
        return builder.create();
    }

    private void restoreTransactions(boolean force) {
        boolean initialized = Preferences.getBoolean(BillingConstants.PREF_TRANSACTIONS_INITIALIZED, false);
        if (!initialized || force) {
            billingService.restoreTransactions();
        }
        if (force) {
            restoreTransactionsDialog = DialogUtilities.progressDialog(this, getString(R.string.premium_checking_for_purchases));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void restoreTransactionsResponse(ResponseCode responseCode) {
        DialogUtilities.dismissDialog(this, restoreTransactionsDialog);
        restoreTransactionsDialog = null;
    }
}
