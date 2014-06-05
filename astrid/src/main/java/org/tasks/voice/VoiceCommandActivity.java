package org.tasks.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;

import org.tasks.R;
import org.tasks.injection.InjectingActivity;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import static com.todoroo.astrid.ui.QuickAddBar.basicQuickAddTask;

public class VoiceCommandActivity extends InjectingActivity {

    @Inject Preferences preferences;
    @Inject GCalHelper gcalHelper;
    @Inject MetadataService metadataService;
    @Inject TagService tagService;
    @Inject TaskService taskService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        switch (intent.getAction()) {
            case "com.google.android.gm.action.AUTO_SEND":
                final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                basicQuickAddTask(preferences, gcalHelper, taskService, metadataService, tagService, text);
                Context context = getApplicationContext();
                if (context != null) {
                    Toast.makeText(context, getString(R.string.voice_command_added_task), Toast.LENGTH_LONG).show();
                }
                finish();
        }
    }
}
