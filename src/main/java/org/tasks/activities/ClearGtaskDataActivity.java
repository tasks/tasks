package org.tasks.activities;

import android.content.DialogInterface;
import android.os.Bundle;

import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;

import org.tasks.R;
import org.tasks.injection.InjectingActivity;

import javax.inject.Inject;

public class ClearGtaskDataActivity extends InjectingActivity {

    @Inject GtasksSyncV2Provider gtasksSyncV2Provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DialogUtilities.okCancelDialog(ClearGtaskDataActivity.this,
                getString(R.string.sync_forget_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gtasksSyncV2Provider.signOut();
                        finish();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }
        );
    }
}
