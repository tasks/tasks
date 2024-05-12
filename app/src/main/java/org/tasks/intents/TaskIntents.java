package org.tasks.intents;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.api.Filter;
import org.tasks.data.entity.Task;

public class TaskIntents {

  public static final int FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP;

  public static Intent getNewTaskIntent(
          Context context,
          @Nullable Filter filter,
          String createSource
  ) {
    Intent intent = new Intent(context, MainActivity.class);
    if (filter != null) {
      intent.putExtra(MainActivity.OPEN_FILTER, filter);
    }
    intent.putExtra(MainActivity.CREATE_TASK, 0L);
    intent.putExtra(MainActivity.CREATE_SOURCE, createSource);
    intent.putExtra(MainActivity.REMOVE_TASK, true);
    return intent;
  }

  public static Intent getEditTaskIntent(Context context, @Nullable Filter filter, Task task) {
    Intent intent = new Intent(context, MainActivity.class);
    if (filter != null) {
      intent.putExtra(MainActivity.OPEN_FILTER, filter);
    }
    intent.putExtra(MainActivity.OPEN_TASK, task);
    intent.putExtra(MainActivity.REMOVE_TASK, true);
    return intent;
  }

  public static Intent getTaskListIntent(Context context, @Nullable Filter filter) {
    Intent intent = new Intent(context, MainActivity.class);
    intent.setFlags(FLAGS);
    if (filter != null) {
      intent.putExtra(MainActivity.OPEN_FILTER, filter);
    }
    return intent;
  }

  public static Intent getTaskListByIdIntent(Context context, @Nullable String filterId) {
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.setComponent(new ComponentName(context, MainActivity.class));
    intent.putExtra(MainActivity.LOAD_FILTER, filterId);
    return intent;
  }
}
