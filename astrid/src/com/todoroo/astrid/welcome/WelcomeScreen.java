package com.todoroo.astrid.welcome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.activity.Eula;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.service.StartupService;

public class WelcomeScreen extends Activity implements Eula.EulaCallback {

    Button showEula;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ContextManager.setContext(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        new StartupService().onStartupApplication(this);
        setContentView(R.layout.welcome_screen);

        if(Preferences.getBoolean(Eula.PREFERENCE_EULA_ACCEPTED, false)) {
            Intent taskListStartup = new Intent(this, TaskListActivity.class);
            startActivity(taskListStartup);
            finish();
            return;
        }

        showEula = (Button) findViewById(R.id.show_eula);
        showEula.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                 Eula.showEula(WelcomeScreen.this);
            }
        });
    }

    @Override
    public void eulaAccepted() {
        Intent login = new Intent(this, ActFmLoginActivity.class);
        login.putExtra(ActFmLoginActivity.KEY_SHOW_LATER_BUTTON, true);
        startActivity(login);
        finish();
    }

    @Override
    public void eulaRefused() {
        // Do nothing
    }
}
