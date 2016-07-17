package org.tasks.files;

import android.os.Bundle;

import com.nononsenseapps.filepicker.FilePickerActivity;

import org.tasks.analytics.Tracker;
import org.tasks.injection.ActivityModule;
import org.tasks.injection.InjectingApplication;
import org.tasks.themes.Theme;

import javax.inject.Inject;

public class MyFilePickerActivity extends FilePickerActivity {

    @Inject Theme theme;
    @Inject Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((InjectingApplication) getApplication())
                .getComponent()
                .plus(new ActivityModule(this))
                .inject(this);
        theme.applyThemeAndStatusBarColor(this, getDelegate());
        setTitle(null);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        tracker.showScreen(getClass().getSimpleName());
    }
}
