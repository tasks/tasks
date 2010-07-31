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
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Preferences;

/**
 * Astrid Service for managing add-ons
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class AddonService {

    /** OEM preference key */
    private static final String PREF_OEM = "poem";

    /** Astrid Power Pack package */
    private static final String POWER_PACK_PACKAGE = "com.todoroo.astrid.ppack";

    /** Astrid Power Pack label */
    public static final String POWER_PACK_LABEL = "Astrid Power Pack";

    /** cached is power pack value */
    private static Boolean isPowerPack = null;

    /** Checks whether power pack should be enabled */
    public static boolean isPowerPack() {
        if (isPowerPack != null)
            return isPowerPack;

        isPowerPack = false;
        if (Preferences.getBoolean(PREF_OEM, false))
            isPowerPack = true;
        else {
            try {
                Context context = ContextManager.getContext();
                ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(
                        POWER_PACK_PACKAGE, 0);
                if(applicationInfo.uid == context.getApplicationInfo().uid)
                    isPowerPack = true;
            } catch (PackageManager.NameNotFoundException e) {
                // not found
            }
        }
        return isPowerPack;
    }

    /** Displays power pack help */
    public static void displayPowerPackHelp(Activity activity) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        ImageView imageView = new ImageView(activity);
        imageView.setImageResource(R.drawable.icon_pp);
        layout.addView(imageView);
        TextView textView = new TextView(activity);
        textView.setText(R.string.DLG_power_pack);
        textView.setTextSize(16);
        layout.addView(textView);

        new AlertDialog.Builder(activity)
        .setTitle(POWER_PACK_LABEL)
        .setView(layout)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(R.string.DLG_to_market, new MarketClickListener(activity,
                POWER_PACK_PACKAGE))
        .show();
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

}
