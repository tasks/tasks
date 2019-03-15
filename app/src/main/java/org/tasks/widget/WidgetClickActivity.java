package org.tasks.widget;

import android.content.Intent;
import android.os.Bundle;
import com.google.common.base.Strings;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.intents.TaskIntents;

public class WidgetClickActivity extends InjectingAppCompatActivity {

  public static final String COMPLETE_TASK = "COMPLETE_TASK";
  public static final String EDIT_TASK = "EDIT_TASK";
  public static final String EXTRA_FILTER = "extra_filter";
  public static final String EXTRA_TASK = "extra_task"; // $NON-NLS-1$

  @Inject TaskDao taskDao;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();

    String action = intent.getAction();

    if (Strings.isNullOrEmpty(action)) {
      return;
    }

    Task task = intent.getParcelableExtra(EXTRA_TASK);

    switch (action) {
      case COMPLETE_TASK:
        taskDao.setComplete(task, !task.isCompleted());
        break;
      case EDIT_TASK:
        startActivity(
            TaskIntents.getEditTaskIntent(
                this,
                intent.getParcelableExtra(EXTRA_FILTER),
                intent.getParcelableExtra(EXTRA_TASK)));
        break;
    }

    finish();
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
