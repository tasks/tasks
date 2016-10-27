package org.tasks.injection;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.tasks.locale.Locale;
import org.tasks.themes.Theme;

import javax.inject.Inject;

public abstract class ThemedInjectingAppCompatActivity extends AppCompatActivity implements InjectingActivity {
    private ActivityComponent activityComponent;

    @Inject Theme theme;

    protected ThemedInjectingAppCompatActivity() {
        Locale.getInstance(this).applyOverrideConfiguration(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activityComponent = ((InjectingApplication) getApplication()).getComponent().plus(new ActivityModule(this));
        inject(activityComponent);
        setTitle(null);
        super.onCreate(savedInstanceState);
        theme.applyTheme(this);
        theme.applyStatusBarColor(this);
    }

    @Override
    public ActivityComponent getComponent() {
        return activityComponent;
    }
}
