package org.tasks.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.todoroo.astrid.service.TaskCreator;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingAppCompatActivity;

import javax.inject.Inject;

public class VoiceCommandActivity extends InjectingAppCompatActivity {

    @Inject TaskCreator taskCreator;
    @Inject @ForApplication Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        switch (intent.getAction()) {
            case "com.google.android.gm.action.AUTO_SEND":
                final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                taskCreator.basicQuickAddTask(text);
                Toast.makeText(context, getString(R.string.voice_command_added_task), Toast.LENGTH_LONG).show();
                finish();
        }
    }
}
