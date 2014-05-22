package org.tasks.voice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;

public class VoiceCommandActivity extends Activity {

    @Autowired TaskService taskService;

    public VoiceCommandActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        switch (intent.getAction()) {
            case "com.google.android.gm.action.AUTO_SEND":
                final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                taskService.save(new Task() {{
                    setTitle(text);
                }});
                Context context = getApplicationContext();
                if (context != null) {
                    Toast.makeText(context, getString(R.string.voice_command_added_task), Toast.LENGTH_LONG).show();
                }
                finish();
        }
    }
}
