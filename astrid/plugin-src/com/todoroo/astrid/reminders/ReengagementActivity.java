package com.todoroo.astrid.reminders;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.ThemeService;

public class ReengagementActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new StartupService().onStartupApplication(this);
        super.onCreate(savedInstanceState);
        DependencyInjectionService.getInstance().inject(this);

        setContentView(R.layout.reengagement_activity);

        setUpUi();
    }

    private void setUpUi() {
        View dismiss1 = findViewById(R.id.dismiss);
        View dismiss2 = findViewById(R.id.dismiss_button);
        OnClickListener dismissListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                AndroidUtilities.callOverridePendingTransition(ReengagementActivity.this, 0, android.R.anim.fade_out);
            }
        };

        dismiss1.setOnClickListener(dismissListener);
        dismiss2.setOnClickListener(dismissListener);

        TextView seeTasksButton = (TextView) findViewById(R.id.see_tasks_button);
        seeTasksButton.setBackgroundColor(getResources().getColor(ThemeService.getThemeColor()));
        seeTasksButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //
            }
        });


        ((TextView) findViewById(R.id.reminder_title)).setText("TITLE");

        if (!Preferences.getBoolean(R.string.p_rmd_nagging, true)) {
            findViewById(R.id.missed_calls_speech_bubble).setVisibility(View.GONE);
        } else {
            TextView dialogView = (TextView) findViewById(R.id.reminder_message);
            dialogView.setText(Notifications.getRandomReminder(getResources().getStringArray(R.array.rmd_reengage_dialog_options)));
        }
    }

}
