package com.todoroo.astrid.welcome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.abtesting.ABChooser;
import com.todoroo.astrid.service.abtesting.ABOptions;
import com.todoroo.astrid.service.abtesting.FeatureFlipper;
import com.todoroo.astrid.utility.AstridPreferences;

public class SplashScreenLauncher extends Activity {

    private boolean shouldGoThroughWelcome = false;
    private int latestSetVersion = 0;

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
        latestSetVersion = AstridPreferences.getCurrentVersion();
        shouldGoThroughWelcome = ((latestSetVersion == 0) || !Preferences.getBoolean(WelcomeLogin.KEY_SHOWED_WELCOME_LOGIN, false));
        ContextManager.setContext(this);
        if (latestSetVersion == 0) {
            new Thread() {
                @Override
                public void run() {
                    new FeatureFlipper().updateFeatures();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new StartupService().onStartupApplication(SplashScreenLauncher.this);
                            finishAndShowNext();
                        }
                    });
                }
            }.start();
        } else {
            new StartupService().onStartupApplication(this);
            finishAndShowNext();
        }
    }

    private void finishAndShowNext() {
        if (shouldGoThroughWelcome) {
            int welcomeLoginChoice = abChooser.getChoiceForOption(ABOptions.AB_OPTION_WELCOME_LOGIN);
            welcomeLoginPath(welcomeLoginChoice);
        } else {
            int tasksOrListsChoice = abChooser.getChoiceForOption(ABOptions.AB_OPTION_FIRST_ACTIVITY);
            mainActivityPath(tasksOrListsChoice);
        }
    }

    private void welcomeLoginPath(int welcomeLoginChoice) {
        Intent intent = new Intent();
        switch(welcomeLoginChoice) {
        case 0: // Show welcome login, then welcome screen
            intent.setClass(this, WelcomeLogin.class);
            break;
        case 1: // Go straight to welcome screen
            intent.setClass(this, WelcomeGraphic.class);
            intent.putExtra(WelcomeGraphic.KEY_SHOW_EULA, true);
            break;
        default:
            intent.setClass(this, WelcomeLogin.class);
            break;
        }
        startActivity(intent);
        finish();
    }

    private void mainActivityPath(int tasksOrListsChoice) {
        Intent intent = new Intent();
        switch (tasksOrListsChoice) {
        case 0:
            intent.setClass(this, TaskListActivity.class); // Go to task list activity
            break;
        case 1:
            intent.setClass(this, FilterListActivity.class); // Go to filter list activity
            intent.putExtra(FilterListActivity.SHOW_BACK_BUTTON, false);
            break;
        default:
            intent.setClass(this, TaskListActivity.class);
            break;
        }
        startActivity(intent);
        finish();
    }

}
