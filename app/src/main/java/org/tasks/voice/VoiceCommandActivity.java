package org.tasks.voice;

import static org.tasks.Strings.isNullOrEmpty;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.todoroo.astrid.service.TaskCreator;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ApplicationContext;
import org.tasks.injection.InjectingAppCompatActivity;

public class VoiceCommandActivity extends InjectingAppCompatActivity {

  @Inject TaskCreator taskCreator;
  @Inject @ApplicationContext Context context;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();

    if ("com.google.android.gm.action.AUTO_SEND".equals(intent.getAction())) {
      final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
      if (!isNullOrEmpty(text)) {
        taskCreator.basicQuickAddTask(text);
        Toast.makeText(context, getString(R.string.voice_command_added_task), Toast.LENGTH_LONG)
            .show();
      }
      finish();
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
