package org.tasks.activities;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;



/**
 * Created by Qi on 2015/11/13.
 */
public class BackupCycleActivity extends InjectingAppCompatActivity {

    @Inject
    Preferences preferences;
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String[] Backup_set = new String[]{"every day","every two days","every three days","every four days","every week"};
        new AlertDialog.Builder(this).setTitle(R.string.backup_cycle_set).setItems(
                Backup_set, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(BackupCycleActivity.this,"All tasks will be backup "
                                + Backup_set[which] + " automatically", Toast.LENGTH_SHORT).show();
                        which ++;
                        if(which == 5) {
                            which = 7;
                        }
                        preferences.setString(R.string.p_backup_cyc, String.valueOf(which++));
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
