package org.tasks.intents;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Task;

public class TaskIntents {

  public static Intent getEditTaskIntent(Context context, Task task) {
    return getEditTaskIntent(context, null, task);
  }

  public static Intent getEditTaskIntent(Context context, @Nullable Filter filter, Task task) {
    Intent intent = getTaskListIntent(context, filter);
    intent.putExtra(MainActivity.OPEN_TASK, task);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    return intent;
  }

  public static Intent getTaskListIntent(Context context, @Nullable Filter filter) {
    Intent intent = new Intent(context, MainActivity.class);
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
