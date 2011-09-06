package com.todoroo.astrid.welcome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.Eula;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.service.StartupService;

public class WelcomeScreen extends Activity {

    public static final String KEY_SHOWED_WELCOME_SCREEN = "key_showed_welcome_screen"; //$NON-NLS-1$

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ContextManager.setContext(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        new StartupService().onStartupApplication(this);
        setContentView(R.layout.welcome_screen);

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

        if(Preferences.getBoolean(KEY_SHOWED_WELCOME_SCREEN, false)) {
            finishAndStartNext();
        }
    }

    private void finishAndStartNext() {
        Intent taskListStartup = new Intent(this, TaskListActivity.class);
        startActivity(taskListStartup);
        finish();
        Preferences.setBoolean(KEY_SHOWED_WELCOME_SCREEN, true);
    }
}
