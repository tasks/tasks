package org.tasks.injection;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.tasks.analytics.Tracker;
import org.tasks.locale.Locale;
import org.tasks.themes.Theme;

import javax.inject.Inject;

public abstract class ThemedInjectingAppCompatActivity extends AppCompatActivity implements InjectingActivity {
    private ActivityComponent activityComponent;

    @Inject Tracker tracker;
    @Inject Theme theme;

    public ThemedInjectingAppCompatActivity() {
        Locale.INSTANCE.applyOverrideConfiguration(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activityComponent = ((InjectingApplication) getApplication()).getComponent().plus(new ActivityModule(this));
        inject(activityComponent);
        setTitle(null);
        theme.applyTheme(this);
        theme.applyStatusBarColor(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        tracker.showScreen(getClass().getSimpleName());
    }

    @Override
    public ActivityComponent getComponent() {
        return activityComponent;
    }
}
