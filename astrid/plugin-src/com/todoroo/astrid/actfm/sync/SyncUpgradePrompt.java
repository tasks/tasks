package com.todoroo.astrid.actfm.sync;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.core.PluginServices;

public class SyncUpgradePrompt {

    private static final String P_SYNC_UPGRADE_PROMPT = "p_sync_upgr_prompt"; //$NON-NLS-1$
    private static long lastPromptDate = -1;

    public static void showSyncUpgradePrompt(final Activity activity) {
        if (lastPromptDate == -1)
            lastPromptDate = Preferences.getLong(P_SYNC_UPGRADE_PROMPT, 0L);

        Dialog d = null;
        if (DateUtilities.now() - lastPromptDate > DateUtilities.ONE_WEEK * 3) {
            if (!PluginServices.getActFmPreferenceService().isLoggedIn()) {
                if (PluginServices.getGtasksPreferenceService().isLoggedIn()) {
                    // Logged into google but not astrid
                    d = getDialog(activity, R.string.sync_upgr_gtasks_only_title, R.string.sync_upgr_gtasks_only_body,
                            R.string.sync_upgr_gtasks_only_btn1, new Runnable() {
                                @Override
                                public void run() {
                                    activity.startActivity(new Intent(activity, ActFmLoginActivity.class));
                                }
                            },
                            R.string.sync_upgr_gtasks_only_btn2,
                            null);
                } else {
                    // Logged into neither
                    d = getDialog(activity, R.string.sync_upgr_neither_title, R.string.sync_upgr_neither_body,
                            R.string.sync_upgr_neither_btn1, new Runnable() {
                                @Override
                                public void run() {
                                    activity.startActivity(new Intent(activity, ActFmLoginActivity.class));
                                }
                            });
                }
                setLastPromptDate(DateUtilities.now());
            } else if (PluginServices.getGtasksPreferenceService().isLoggedIn()) {
                // Logged into both
                d = getDialog(activity, R.string.sync_upgr_both_title, R.string.sync_upgr_both_body,
                        R.string.sync_upgr_both_btn1,
                        null,
                        R.string.sync_upgr_both_btn2, new Runnable() {
                            @Override
                            public void run() {
                                new ActFmSyncV2Provider().signOut(activity);
                                Toast.makeText(activity, R.string.sync_upgr_logged_out, Toast.LENGTH_LONG).show();
                            }
                        });
                setLastPromptDate(Long.MAX_VALUE);
            } else {
                // Logged into just astrid--don't need to show prompts anymore
                setLastPromptDate(Long.MAX_VALUE);
            }
        }

        if (d != null)
            d.show();
    }

    private static void setLastPromptDate(long date) {
        lastPromptDate = date;
        Preferences.setLong(P_SYNC_UPGRADE_PROMPT, lastPromptDate);
    }

    private static Dialog getDialog(Activity activity, int title, int body, Object... buttonsAndListeners) {
        final Dialog d = new Dialog(activity, R.style.ReminderDialog);
        d.setContentView(R.layout.astrid_reminder_view_portrait);
        ((TextView) d.findViewById(R.id.reminder_title)).setText(title);
        ((TextView) d.findViewById(R.id.reminder_message)).setText(body);

        d.findViewById(R.id.dismiss).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });

        d.findViewById(R.id.reminder_complete).setVisibility(View.GONE);
        TypedValue tv = new TypedValue();
        activity.getTheme().resolveAttribute(R.attr.asThemeTextColor, tv, false);

        int button1 = (Integer) buttonsAndListeners[0];
        final Runnable listener1 = (Runnable) buttonsAndListeners[1];
        Button b1 = (Button) d.findViewById(R.id.reminder_edit);
        b1.setText(button1);
        b1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                if (listener1 != null)
                    listener1.run();
            }
        });
        b1.setBackgroundColor(activity.getResources().getColor(tv.data));

        if (buttonsAndListeners.length < 3) {
            d.findViewById(R.id.reminder_snooze).setVisibility(View.GONE);
        } else {
            int button2 = (Integer) buttonsAndListeners[2];
            final Runnable listener2 = (Runnable) buttonsAndListeners[3];
            Button b2 = (Button) d.findViewById(R.id.reminder_snooze);
            b2.setText(button2);
            b2.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    d.dismiss();
                    if (listener2 != null)
                        listener2.run();
                }
            });
            b2.setBackgroundColor(activity.getResources().getColor(tv.data));
        }

        return d;
    }

}
