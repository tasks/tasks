package org.tasks.activities;

import android.content.DialogInterface;
import android.os.Bundle;

import com.todoroo.astrid.dao.Database;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class ClearAllDataActivity extends InjectingAppCompatActivity {

    @Inject Database database;
    @Inject Preferences preferences;
    @Inject DialogBuilder dialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialogBuilder.newMessageDialog(R.string.EPr_manage_clear_all_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteDatabase(database.getName());
                        preferences.reset();
                        System.exit(0);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();
    }
}
