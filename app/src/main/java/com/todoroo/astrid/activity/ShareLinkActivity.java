package com.todoroo.astrid.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskCreator;

import org.tasks.data.TaskAttachment;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;

import java.util.ArrayList;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastMarshmallow;
import static org.tasks.intents.TaskIntents.getEditTaskStack;

/**
 * @author joshuagross
 *     <p>Create a new task based on incoming links from the "share" menu
 */
public final class ShareLinkActivity extends InjectingAppCompatActivity {

  @Inject TaskCreator taskCreator;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    readIntent();
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    setIntent(intent);

    readIntent();
  }

  private void readIntent() {
    Intent intent = getIntent();
    String action = intent.getAction();

    if (atLeastMarshmallow() && Intent.ACTION_PROCESS_TEXT.equals(action)) {
      CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
      if (text != null) {
        Task task = taskCreator.createWithValues(null, text.toString());
        getEditTaskStack(this, null, task).startActivities();
      }
    } else if (action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE)) {
      String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
      Task task = taskCreator.createWithValues(null, subject);
      task.setNotes(intent.getStringExtra(Intent.EXTRA_TEXT));
      task.putTransitory(TaskAttachment.KEY, getAttachments(intent));
      getEditTaskStack(this, null, task).startActivities();
    } else {
      Timber.e("Unhandled intent: %s", intent);
    }
    finish();
  }

  private ArrayList<Uri> getAttachments(Intent intent) {
    String type = intent.getType();
    if (type != null) {
      String action = intent.getAction();
      if (action.equals(Intent.ACTION_SEND)) {
        if (type.startsWith("image/")) {
          return newArrayList(intent.<Uri>getParcelableExtra(Intent.EXTRA_STREAM));
        }
      } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
        if (type.startsWith("image/")) {
          return intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }
      }
    }
    return new ArrayList<>();
  }
}
