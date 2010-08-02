package com.todoroo.astrid.service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.model.AddOn;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Preferences;

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

    /** cached is power pack value */
    private static Boolean isPowerPack = null;

    /** Checks whether power pack should be enabled */
    public boolean isPowerPack() {
        if (isPowerPack == null) {
            isPowerPack = false;
            if (Preferences.getBoolean(PREF_OEM, false))
                isPowerPack = true;
            else if(isInstalled(POWER_PACK_PACKAGE, true))
                isPowerPack = true;
        }

        return isPowerPack;
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
        final AtomicInteger countdown = new AtomicInteger(10);
        if(DateUtilities.now() > Constants.UPGRADE.getTime()) {

            final AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle(R.string.DLG_information_title)
            .setMessage(R.string.DLG_please_update)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.DLG_to_market,
                    new MarketClickListener(activity, activity.getPackageName()))
            .setNegativeButton(countdown.toString(), null)
            .setCancelable(false)
            .show();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

            final Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    final int number = countdown.addAndGet(-1);
                    if(number == 0)
                        timer.cancel();

                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            if(number == 0) {
                                dialog.setCancelable(true);
                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(
                                        android.R.string.ok);
                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                            } else {
                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(
                                        Integer.toString(number));
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
        return isInstalled(addOn.getPackageName(), addOn.isInternal());
    }

    /**
     * Check whether a given add-on is installed
     * @param addOn
     * @return
     */
    private boolean isInstalled(String packageName, boolean internal) {
        Context context = ContextManager.getContext();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(
                    packageName, 0);
        } catch (Exception e) {
            return false;
        }

        if(applicationInfo == null)
            return false;
        if(!internal)
            return true;
        return applicationInfo.uid == context.getApplicationInfo().uid;
    }

    /**
     * Get a list of add-ons
     *
     * @return available add-ons
     */
    public AddOn[] getAddOns() {
        Resources r = ContextManager.getContext().getResources();

        // temporary temporary
        AddOn[] list = new AddOn[2];
        list[0] = new AddOn(false, true, "Astrid Power Pack", null,
                "Support Astrid and get more productive with the Astrid Power Pack backup, widgets, no ads, and calendar integration. Power up today!",
                POWER_PACK_PACKAGE, "http://www.weloveastrid.com/store",
                ((BitmapDrawable)r.getDrawable(R.drawable.icon_pp)).getBitmap());

        list[1] = new AddOn(false, true, "Astrid Locale Plugin", null,
                "Allows Astrid to make use of the Locale application to send you notifications based on filter conditions",
                LOCALE_PACKAGE, "http://www.weloveastrid.com/store",
                ((BitmapDrawable)r.getDrawable(R.drawable.icon_pp)).getBitmap());

        return list;
    }
}
