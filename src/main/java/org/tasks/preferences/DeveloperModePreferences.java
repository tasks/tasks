package org.tasks.preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import com.todoroo.astrid.backup.TasksXmlImporter;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;

import javax.inject.Inject;

/**
 * Created by Qi on 2015/11/17.
 */
public class DeveloperModePreferences extends InjectingPreferenceActivity {
    @Inject
    Preferences preferences;
    @Inject
    TasksXmlImporter xmlImporter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            new AlertDialog.Builder(this).setTitle(R.string.dev_mode_title)
                    .setMessage(R.string.dev_mode_on).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    xmlImporter.importTasks(DeveloperModePreferences.this,"dev_mode", new Runnable() {
                        //Environment.getExternalStorageDirectory().getAbsolutePath()+ "/astrid/user.151117-2352.xml"
                        //"/storage/sdcard/astrid/user.151117-2352.xml"
                        @Override
                        public void run() {
                            Flags.set(Flags.REFRESH);
                            finish();
                        }
                    });
                    Toast.makeText(DeveloperModePreferences.this, R.string.dev_mode_on_succ, Toast.LENGTH_SHORT).show();
                    preferences.setString(R.string.dev_mode_set, "1");

                    finish();
                }
            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    }).show();
        }

    }
