package org.tasks.activities;

import android.content.DialogInterface;
import android.os.Bundle;

import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingAppCompatActivity;

import javax.inject.Inject;

public class ClearGtaskDataActivity extends InjectingAppCompatActivity {

    @Inject GtasksSyncV2Provider gtasksSyncV2Provider;
    @Inject DialogBuilder dialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialogBuilder.newMessageDialog(R.string.sync_forget_confirm)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gtasksSyncV2Provider.signOut();
                        setResult(RESULT_OK);
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
