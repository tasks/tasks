package com.todoroo.astrid.tags;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.data.RemoteModel;

import org.tasks.R;
import org.tasks.injection.InjectingActivity;

import javax.inject.Inject;

public abstract class TagActivity extends InjectingActivity {

    String tag;
    String uuid;

    @Inject TagService tagService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tag = getIntent().getStringExtra(TagFilterExposer.TAG);
        uuid = getIntent().getStringExtra(TagViewFragment.EXTRA_TAG_UUID);

        if(tag == null || RemoteModel.isUuidEmpty(uuid)) {
            finish();
            return;
        }

        showDialog();
    }

    protected DialogInterface.OnClickListener getOkListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Intent result = ok();
                    if (result != null) {
                        setResult(RESULT_OK, result);
                    } else {
                        toastNoChanges();
                        setResult(RESULT_CANCELED);
                    }
                } finally {
                    finish();
                }
            }
        };
    }

    protected DialogInterface.OnClickListener getCancelListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    toastNoChanges();
                } finally {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }

        };
    }

    private void toastNoChanges() {
        Toast.makeText(this, R.string.TEA_no_tags_modified,
                Toast.LENGTH_SHORT).show();
    }

    protected abstract void showDialog();

    protected abstract Intent ok();
}

