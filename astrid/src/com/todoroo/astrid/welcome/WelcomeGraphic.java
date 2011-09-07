package com.todoroo.astrid.welcome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.Eula;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.abtesting.ABChooser;
import com.todoroo.astrid.service.abtesting.ABOptions;

public class WelcomeGraphic extends Activity {

    public static final String KEY_SHOW_EULA = "show_eula"; //$NON-NLS-1$

    @Autowired ABChooser abChooser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ContextManager.setContext(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        DependencyInjectionService.getInstance().inject(this);

        super.onCreate(savedInstanceState);
        new StartupService().onStartupApplication(this);
        setContentView(R.layout.welcome_screen);

        if (getIntent().getBooleanExtra(KEY_SHOW_EULA, false))
            Eula.showEula(this);

        final ImageView image = (ImageView)findViewById(R.id.welcome_image);
        image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                image.setOnClickListener(null); // Prevent double click
                new Thread() {
                    @Override
                    public void run() {
                        AndroidUtilities.sleepDeep(1000L);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                finishAndStartNext();
                            }
                        });
                    }
                }.start();
            }
        });
    }

    @Override
    protected void onPause() {
        StatisticsService.sessionPause();
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatisticsService.sessionStart(this);
    }

    @Override
    protected void onStop() {
        StatisticsService.sessionStop(this);
        super.onStop();
    }

    private void finishAndStartNext() {
        Intent nextActivity = getNextIntent();
        startActivity(nextActivity);
        finish();
    }

    private Intent getNextIntent() {
        Intent intent = new Intent();
        int choice = abChooser.getChoiceForOption(ABOptions.AB_OPTION_FIRST_ACTIVITY);
        switch (choice) {
        case 0:
            intent.setClass(this, TaskListActivity.class);
            break;
        case 1:
            intent.setClass(this, FilterListActivity.class);
            intent.putExtra(FilterListActivity.SHOW_BACK_BUTTON, false);
            break;
        default:
            intent.setClass(this, TaskListActivity.class);
        }
        return intent;
    }
}
