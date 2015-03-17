package org.tasks.activities;

import android.content.DialogInterface;
import android.os.Bundle;

import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.Database;

import org.tasks.R;
import org.tasks.injection.InjectingActivity;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class ClearAllDataActivity extends InjectingActivity {

    @Inject Database database;
    @Inject Preferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DialogUtilities.okCancelDialog(
                this,
                getResources().getString(R.string.EPr_manage_clear_all_message),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteDatabase(database.getName());

                        preferences.reset();

                        System.exit(0);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
    }
}
