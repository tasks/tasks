package org.tasks.files;

import android.os.Bundle;

import com.nononsenseapps.filepicker.FilePickerActivity;

import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.ThemeApplicator;
import org.tasks.preferences.ThemeManager;

public class MyFilePickerActivity extends FilePickerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PermissionChecker permissionChecker = new PermissionChecker(this);
        Preferences preferences = new Preferences(this, permissionChecker);
        ThemeManager themeManager = new ThemeManager(this, preferences);
        ThemeApplicator themeApplicator = new ThemeApplicator(this, themeManager);
        themeApplicator.applyThemeAndStatusBarColor();

        super.onCreate(savedInstanceState);
    }
}
