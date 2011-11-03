package com.todoroo.astrid.welcome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.abtesting.ABChooser;
import com.todoroo.astrid.utility.AstridPreferences;

public class SplashScreenLauncher extends Activity {

    @Autowired ABChooser abChooser;

    static {
        AstridDependencyInjector.initialize();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        DependencyInjectionService.getInstance().inject(this);
        setContentView(R.layout.splash_screen_launcher);
        int latestSetVersion = AstridPreferences.getCurrentVersion();
        boolean isNewUser = (latestSetVersion == 0);
        ContextManager.setContext(this);
        new StartupService().onStartupApplication(this);
        StatisticsService.sessionStart(this);
        finishAndShowNext(isNewUser);
        StatisticsService.sessionStop(this);
    }

    private void finishAndShowNext(boolean isNewUser) {
        if (isNewUser) {
            welcomeLoginPath();
        } else {
            mainActivityPath();
        }
    }

    private void welcomeLoginPath() {
        Intent intent = new Intent();
        intent.setClass(this, WelcomeLogin.class);
        startActivity(intent);
        finish();
    }

    private void mainActivityPath() {
        Intent intent = new Intent();
        intent.setClass(this, TaskListActivity.class); // Go to task list activity
        startActivity(intent);
        finish();
    }

}
