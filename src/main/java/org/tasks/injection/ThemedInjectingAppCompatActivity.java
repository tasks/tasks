package org.tasks.injection;

import android.os.Bundle;

import org.tasks.preferences.ThemeApplicator;

import javax.inject.Inject;

public abstract class ThemedInjectingAppCompatActivity extends InjectingAppCompatActivity {

    @Inject ThemeApplicator themeApplicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themeApplicator.applyThemeAndStatusBarColor();
    }
}
