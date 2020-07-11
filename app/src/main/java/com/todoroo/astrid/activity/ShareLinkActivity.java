package com.todoroo.astrid.activity;

import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.ACTION_SEND_MULTIPLE;
import static com.google.common.collect.Lists.newArrayList;
import static org.tasks.Strings.isNullOrEmpty;
import static org.tasks.files.FileHelper.copyToUri;
import static org.tasks.files.FileHelper.getFilename;
import static org.tasks.intents.TaskIntents.getEditTaskIntent;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.common.io.Files;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskCreator;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.ArrayList;
import javax.inject.Inject;
import org.tasks.data.TaskAttachment;
import org.tasks.files.FileHelper;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

/**
 * @author joshuagross
 *     <p>Create a new task based on incoming links from the "share" menu
 */
@AndroidEntryPoint
public final class ShareLinkActivity extends InjectingAppCompatActivity {

  @Inject @ApplicationContext Context context;
  @Inject TaskCreator taskCreator;
  @Inject Preferences preferences;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    String action = intent.getAction();

    if (Intent.ACTION_PROCESS_TEXT.equals(action)) {
      CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
      if (text != null) {
        Task task = taskCreator.createWithValues(text.toString());
        editTask(task);
      }
    } else if (ACTION_SEND.equals(action)) {
      String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
      Task task = taskCreator.createWithValues(subject);
      task.setNotes(intent.getStringExtra(Intent.EXTRA_TEXT));
      if (hasAttachments(intent)) {
        task.putTransitory(TaskAttachment.KEY, copyAttachment(intent));
      }
      editTask(task);
    } else if (ACTION_SEND_MULTIPLE.equals(action)) {
      Task task = taskCreator.createWithValues(intent.getStringExtra(Intent.EXTRA_SUBJECT));
      task.setNotes(intent.getStringExtra(Intent.EXTRA_TEXT));
      if (hasAttachments(intent)) {
        task.putTransitory(TaskAttachment.KEY, copyMultipleAttachments(intent));
      }
      editTask(task);
    } else {
      Timber.e("Unhandled intent: %s", intent);
    }
    finish();
  }

  private void editTask(Task task) {
    Intent intent = getEditTaskIntent(this, null, task);
    intent.putExtra(MainActivity.FINISH_AFFINITY, true);
    startActivity(intent);
  }

  private ArrayList<Uri> copyAttachment(Intent intent) {
    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
    String filename = getFilename(context, uri);
    if (isNullOrEmpty(filename)) {
      String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
      filename =
          isNullOrEmpty(subject)
              ? uri.getLastPathSegment()
              : subject.substring(0, Math.min(subject.length(), FileHelper.MAX_FILENAME_LENGTH));
    }
    String basename = Files.getNameWithoutExtension(filename);
    return newArrayList(copyToUri(context, preferences.getAttachmentsDirectory(), uri, basename));
  }

  private ArrayList<Uri> copyMultipleAttachments(Intent intent) {
    ArrayList<Uri> result = new ArrayList<>();
    ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
    if (uris != null) {
      for (Uri uri : uris) {
        result.add(copyToUri(context, preferences.getAttachmentsDirectory(), uri));
      }
    }
    return result;
  }

  private boolean hasAttachments(Intent intent) {
    String type = intent.getType();
    return type != null && (type.startsWith("image/") || type.startsWith("application/"));
  }
}
