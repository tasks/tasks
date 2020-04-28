package org.tasks.widget;

import static org.tasks.Strings.isNullOrEmpty;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskCompleter;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.dialogs.DateTimePicker;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.Preferences;

public class WidgetClickActivity extends InjectingAppCompatActivity
    implements DateTimePicker.OnDismissHandler {

  public static final String COMPLETE_TASK = "COMPLETE_TASK";
  public static final String EDIT_TASK = "EDIT_TASK";
  public static final String TOGGLE_SUBTASKS = "TOGGLE_SUBTASKS";
  public static final String RESCHEDULE_TASK = "RESCHEDULE_TASK";
  public static final String EXTRA_FILTER = "extra_filter";
  public static final String EXTRA_TASK = "extra_task"; // $NON-NLS-1$
  public static final String EXTRA_COLLAPSED = "extra_collapsed";
  private static final String FRAG_TAG_DATE_TIME_PICKER = "frag_tag_date_time_picker";

  @Inject TaskCompleter taskCompleter;
  @Inject TaskDao taskDao;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject Preferences preferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();

    String action = intent.getAction();

    if (isNullOrEmpty(action)) {
      return;
    }

    Task task = intent.getParcelableExtra(EXTRA_TASK);

    switch (action) {
      case COMPLETE_TASK:
        taskCompleter.setComplete(task, !task.isCompleted());
        finish();
        break;
      case EDIT_TASK:
        startActivity(
            TaskIntents.getEditTaskIntent(
                this,
                intent.getParcelableExtra(EXTRA_FILTER),
                intent.getParcelableExtra(EXTRA_TASK)));
        finish();
        break;
      case TOGGLE_SUBTASKS:
        taskDao.setCollapsed(task.getId(), intent.getBooleanExtra(EXTRA_COLLAPSED, false));
        localBroadcastManager.broadcastRefresh();
        finish();
        break;
      case RESCHEDULE_TASK:
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_TIME_PICKER) == null) {
          DateTimePicker.Companion.newDateTimePicker(
              task.getId(),
              task.getDueDate(),
              preferences.getBoolean(R.string.p_auto_dismiss_datetime_widget, false))
              .show(fragmentManager, FRAG_TAG_DATE_TIME_PICKER);
        }
        break;
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public void onDismiss() {
    finish();
  }
}
