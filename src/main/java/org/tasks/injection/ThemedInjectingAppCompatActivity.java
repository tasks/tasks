package org.tasks.injection;

import android.os.Bundle;

import org.tasks.themes.Theme;

import javax.inject.Inject;

public abstract class ThemedInjectingAppCompatActivity extends InjectingAppCompatActivity {

    @Inject Theme theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        theme.applyThemeAndStatusBarColor(this);
    }
}
