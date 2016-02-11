package org.tasks.files;

import android.os.Bundle;

import com.nononsenseapps.filepicker.FilePickerActivity;

import org.tasks.preferences.ActivityPreferences;
import org.tasks.preferences.PermissionChecker;

public class MyFilePickerActivity extends FilePickerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityPreferences activityPreferences = new ActivityPreferences(this, new PermissionChecker(this));
        activityPreferences.applyThemeAndStatusBarColor();

        super.onCreate(savedInstanceState);
    }
}
