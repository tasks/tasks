/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.todoroo.astrid.R;

public class StartupReceiver extends BroadcastReceiver {

    public static void setPreferenceDefaults(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = prefs.edit();
        Resources r = context.getResources();

        if(!prefs.contains(r.getString(R.string.rmilk_MPr_interval_key))) {
            editor.putString(r.getString(R.string.rmilk_MPr_interval_key),
                    Integer.toString(0));
        }
        if(!prefs.contains(r.getString(R.string.rmilk_MPr_shortcut_key))) {
            editor.putBoolean(r.getString(R.string.rmilk_MPr_shortcut_key), true);
        }

        editor.commit();
    }

    @Override
    /** Called when this plug-in run for the first time (installed, upgrade, or device was rebooted */
    public void onReceive(final Context context, Intent intent) {
        setPreferenceDefaults(context);
    }

}
