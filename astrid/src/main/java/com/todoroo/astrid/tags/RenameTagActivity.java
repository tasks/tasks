package com.todoroo.astrid.tags;

import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.AstridApiConstants;

import org.tasks.R;

public class RenameTagActivity extends TagActivity {

    private EditText editor;

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
            int renamed = tagService.rename(uuid, text);
            Toast.makeText(this, getString(R.string.TEA_tags_renamed, tag, text, renamed),
                    Toast.LENGTH_SHORT).show();

            if (renamed > 0) {
                Intent intent = new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_RENAMED);
                intent.putExtra(TagViewFragment.EXTRA_TAG_UUID, uuid);
                ContextManager.getContext().sendBroadcast(intent);
                return intent;
            }
            return null;
        }
    }
}