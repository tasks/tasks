package com.todoroo.astrid.service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.AddOn;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.andlib.utility.Preferences;

/**
 * Astrid Service for managing add-ons
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class AddOnService {

    /** OEM preference key */
    private static final String PREF_OEM = "poem";

    /** Astrid Power Pack package */
    public static final String POWER_PACK_PACKAGE = "com.todoroo.astrid.ppack";

    /** Astrid Locale package */
    public static final String LOCALE_PACKAGE = "com.todoroo.astrid.locale";

    /** Astrid Power Pack label */
    public static final String POWER_PACK_LABEL = "Astrid Power Pack";

    /** Checks whether power pack should be enabled */
    public boolean hasPowerPack() {
        if (Preferences.getBoolean(PREF_OEM, false))
            return true;
        else if(isInstalled(POWER_PACK_PACKAGE, true))
            return true;
        return false;
    }

    /** Checks whether locale plugin should be enabled */
    public boolean hasLocalePlugin() {
        if (Preferences.getBoolean(PREF_OEM, false))
            return true;
        else if(isInstalled(LOCALE_PACKAGE, true))
            return true;
        return false;
    }

    /**
     * Takes users to the market
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class MarketClickListener implements DialogInterface.OnClickListener {
        private final Context context;
        private final String packageName;

        public MarketClickListener(Context activity, String packageName) {
            this.context = activity;
            this.packageName = packageName;
        }

        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=pname:" + //$NON-NLS-1$
                            packageName)));
            if(context instanceof Activity)
                ((Activity)context).finish();
        }
    };

    public static void checkForUpgrades(final Activity activity) {
        if(DateUtilities.now() > Constants.UPGRADE.getTime()) {
            final AtomicInteger countdown = new AtomicInteger(10);
            final AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle(R.string.DLG_information_title)
            .setMessage(R.string.DLG_please_update)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.DLG_to_market,
                    new MarketClickListener(activity, activity.getPackageName()))
            .setNegativeButton(countdown.toString(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface myDialog, int which) {
                    // do nothing!
                }
            })
            .setCancelable(false)
            .show();
            dialog.setOwnerActivity(activity);
            dialog.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(false);

            final Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    final int number = countdown.addAndGet(-1);

                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            Button negativeButton =
                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                            if(negativeButton == null)
                                return;
                            if(number == 0)
                                timer.cancel();

                            if(number == 0) {
                                dialog.setCancelable(true);
                                negativeButton.setText(
                                        android.R.string.ok);
                                negativeButton.setEnabled(true);
                            } else {
                                negativeButton.setEnabled(false);
                                negativeButton.setText(Integer.toString(number));
                            }
                        }
                    });
                }
            }, 0L, 1000L);
        }
    }

    /**
     * Record that a version was an OEM install
     */
    public static void recordOem() {
        Preferences.setBoolean(PREF_OEM, true);
    }

    /**
     * Check whether a given add-on is installed
     * @param addOn
     * @return
     */
    public boolean isInstalled(AddOn addOn) {
        // it isnt installed if it is null...
        if (addOn == null)
            return false;
        return isInstalled(addOn.getPackageName(), addOn.isInternal());
    }

    /**
     * Check whether a given add-on is installed
     * @param addOn
     * @return
     */
    private boolean isInstalled(String packageName, boolean internal) {
        if(POWER_PACK_PACKAGE.equals(packageName))
            return true;
        if(LOCALE_PACKAGE.equals(packageName))
            return true;
        if(Constants.PACKAGE.equals(packageName))
            return true;

        Context context = ContextManager.getContext();
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES);
        } catch (Exception e) {
            return false;
        }

        if(packageInfo == null)
            return false;
        if(!internal)
            return true;

        return "30820265308201cea00302010202044954bd9c300d06092a864886f70d01010505003076310b3009060355040613025553310b3009060355040813024341311230100603550407130950616c6f20416c746f31183016060355040a130f6173747269642e6c7632352e636f6d311b3019060355040b131241737472696420446576656c6f706d656e74310f300d0603550403130654696d2053753020170d3038313232363131313835325a180f32303633303932393131313835325a3076310b3009060355040613025553310b3009060355040813024341311230100603550407130950616c6f20416c746f31183016060355040a130f6173747269642e6c7632352e636f6d311b3019060355040b131241737472696420446576656c6f706d656e74310f300d0603550403130654696d20537530819f300d06092a864886f70d010101050003818d00308189028181008b8f39e02a50e5f50723bb71208e99bd72dd3cb6266054809cce0dc33a38ebf79c2a1ab74264cc6c88d44a5092e34f45fc28c53188ebe5b7511f0e14862598a82e1a84b0c99e62b0603737c09501b92f723d9e561a0eedbc16ab494e93a513d170135e0e55af6bb40a9af1186df4cfe53ec3a6144336f9f8a338341656c5a3bd0203010001300d06092a864886f70d01010505000381810016352860629e5e17d2d747943170ddb8c01f014932cb4462f52295c2f764970e93fa461c73b44a678ecf8ab8480702fb746221a98ade8ab7562cae151be78973dfa47144d70b8d0b73220dd741755f62cc9230264f570ec21a4ab1f11b0528d799d3662d06354b56d0d7d28d05c260876a98151fb4e89b6ce2a5010c52b3e365".equals(packageInfo.signatures[0].toCharsString());
    }

    /**
     * Get one AddOn-descriptor by packageName and title.
     *
     * @param packageName could be Constants.PACKAGE or one of AddOnService-constants
     * @param title the descriptive title, as in "Producteev" or "Astrid Power Pack"
     * @return the addon-descriptor, if it is available (registered here in getAddOns), otherwise null
     */
    public AddOn getAddOn(String packageName, String title) {
        if (title == null || packageName == null)
            return null;

        AddOn addon = null;
        AddOn[] addons = getAddOns();
        for (int i = 0; i < addons.length ; i++) {
            if (packageName.equals(addons[i].getPackageName()) && title.equals(addons[i].getTitle())) {
                addon = addons[i];
            }
        }
        return addon;
    }

    /**
     * Get a list of add-ons
     *
     * @return available add-ons
     */
    public AddOn[] getAddOns() {
        Resources r = ContextManager.getContext().getResources();

        // temporary temporary
        AddOn[] list = new AddOn[4];
        list[0] = new AddOn(false, true, "Astrid Power Pack", null,
                "Support Astrid and get more productive with the Astrid Power Pack. Backup, widgets, no ads, and calendar integration. Power up today!",
                POWER_PACK_PACKAGE, "http://www.weloveastrid.com/store",
                ((BitmapDrawable)r.getDrawable(R.drawable.icon_pp)).getBitmap());

        list[1] = new AddOn(false, true, "Astrid Locale Plugin", null,
                "Allows Astrid to make use of the Locale application to send you notifications based on filter conditions. Requires Locale.",
                LOCALE_PACKAGE, "http://www.weloveastrid.com/store",
                ((BitmapDrawable)r.getDrawable(R.drawable.icon_locale)).getBitmap());

        list[2] = new AddOn(true, true, "Producteev", null,
                "Synchronize with Producteev service. Also changes Astrid's importance levels to stars.",
                Constants.PACKAGE, "http://www.producteev.com",
                ((BitmapDrawable)r.getDrawable(R.drawable.icon_producteev)).getBitmap());

        list[3] = new AddOn(true, false, "Remember the Milk", null,
                "Synchronize with Remember The Milk service.",
                Constants.PACKAGE, "http://www.rmilk.com",
                ((BitmapDrawable)r.getDrawable(R.drawable.ic_menu_refresh)).getBitmap());

        return list;
    }
}
