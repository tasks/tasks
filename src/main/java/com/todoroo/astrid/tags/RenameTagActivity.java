package com.todoroo.astrid.tags;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.AstridApiConstants;

import org.tasks.R;
import org.tasks.injection.ForApplication;

import javax.inject.Inject;

public class RenameTagActivity extends TagActivity {

    private EditText editor;

    @Inject TagService tagService;
    @Inject @ForApplication Context context;

    @Override
    protected void showDialog() {
        editor = new EditText(this);
        DialogUtilities.viewDialog(this, getString(R.string.DLG_rename_this_tag_header, tag), editor, getOkListener(), getCancelListener());
    }

    @Override
    protected Intent ok() {
        if(editor == null) {
            return null;
        }

        String text = editor.getText().toString();
        if (text == null || text.length() == 0) {
            return null;
        } else {
            int tasksAffected = tagService.rename(uuid, text);
            Toast.makeText(this, getString(R.string.TEA_tags_renamed, tag, text, tasksAffected),
                    Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_RENAMED) {{
                putExtra(TagViewFragment.EXTRA_TAG_UUID, uuid);
            }};
            context.sendBroadcast(intent);
            return intent;
        }
    }
}