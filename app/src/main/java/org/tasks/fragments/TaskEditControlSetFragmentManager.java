package org.tasks.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.HideUntilControlSet;
import com.todoroo.astrid.ui.ReminderControlSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.preferences.Preferences;
import org.tasks.sync.SyncAdapters;
import org.tasks.ui.CalendarControlSet;
import org.tasks.ui.DeadlineControlSet;
import org.tasks.ui.DescriptionControlSet;
import org.tasks.ui.PriorityControlSet;
import org.tasks.ui.RemoteListFragment;
import org.tasks.ui.TaskEditControlFragment;

public class TaskEditControlSetFragmentManager {

  public static final int[] TASK_EDIT_CONTROL_FRAGMENT_ROWS =
      new int[] {
        R.id.row_title,
        R.id.comment_bar,
        R.id.row_1,
        R.id.row_2,
        R.id.row_3,
        R.id.row_4,
        R.id.row_5,
        R.id.row_6,
        R.id.row_7,
        R.id.row_8,
        R.id.row_9,
        R.id.row_10,
        R.id.row_11
      };

  private static final int[] TASK_EDIT_CONTROL_SET_FRAGMENTS =
      new int[] {
        EditTitleControlSet.TAG,
        DeadlineControlSet.TAG,
        TimerControlSet.TAG,
        DescriptionControlSet.TAG,
        CalendarControlSet.TAG,
        PriorityControlSet.TAG,
        HideUntilControlSet.TAG,
        ReminderControlSet.TAG,
        FilesControlSet.TAG,
        TagsControlSet.TAG,
        RepeatControlSet.TAG,
        CommentBarFragment.TAG,
        RemoteListFragment.TAG
      };

  static {
    //noinspection ConstantConditions
    if (BuildConfig.DEBUG
        && TASK_EDIT_CONTROL_FRAGMENT_ROWS.length != TASK_EDIT_CONTROL_SET_FRAGMENTS.length) {
      throw new AssertionError();
    }
  }

  private final Map<String, Integer> controlSetFragments = new LinkedHashMap<>();
  private final List<String> displayOrder;
  private final SyncAdapters syncAdapters;
  private int numRows;

  public TaskEditControlSetFragmentManager(
      Activity activity, Preferences preferences, SyncAdapters syncAdapters) {
    this.syncAdapters = syncAdapters;
    displayOrder = BeastModePreferences.constructOrderedControlList(preferences, activity);
    displayOrder.add(0, activity.getString(EditTitleControlSet.TAG));
    displayOrder.add(1, activity.getString(CommentBarFragment.TAG));
    String hideAlwaysTrigger = activity.getString(R.string.TEA_ctrl_hide_section_pref);
    for (numRows = 0; numRows < displayOrder.size(); numRows++) {
      if (displayOrder.get(numRows).equals(hideAlwaysTrigger)) {
        break;
      }
    }

    for (int resId : TASK_EDIT_CONTROL_SET_FRAGMENTS) {
      controlSetFragments.put(activity.getString(resId), resId);
    }
  }

  public List<TaskEditControlFragment> getFragmentsInPersistOrder(FragmentManager fragmentManager) {
    List<TaskEditControlFragment> fragments = new ArrayList<>();
    for (String tag : controlSetFragments.keySet()) {
      TaskEditControlFragment fragment =
          (TaskEditControlFragment) fragmentManager.findFragmentByTag(tag);
      if (fragment != null) {
        fragments.add(fragment);
      }
    }
    return fragments;
  }

  public List<TaskEditControlFragment> getOrCreateFragments(
      TaskEditFragment taskEditFragment, Task task) {
    Bundle arguments = new Bundle();
    arguments.putParcelable(TaskEditControlFragment.EXTRA_TASK, task);

    List<TaskEditControlFragment> fragments = new ArrayList<>();
    FragmentManager fragmentManager = taskEditFragment.getChildFragmentManager();
    for (int i = 0; i < numRows; i++) {
      String tag = displayOrder.get(i);
      TaskEditControlFragment fragment =
          (TaskEditControlFragment) fragmentManager.findFragmentByTag(tag);
      if (fragment == null) {
        Integer resId = controlSetFragments.get(tag);
        fragment = createFragment(resId);
        if (fragment == null) {
          continue;
        }
        fragment.setArguments(arguments);
      }
      fragments.add(fragment);
    }
    return fragments;
  }

  private TaskEditControlFragment createFragment(int fragmentId) {
    switch (fragmentId) {
      case EditTitleControlSet.TAG:
        return new EditTitleControlSet();
      case DeadlineControlSet.TAG:
        return new DeadlineControlSet();
      case PriorityControlSet.TAG:
        return new PriorityControlSet();
      case DescriptionControlSet.TAG:
        return new DescriptionControlSet();
      case CalendarControlSet.TAG:
        return new CalendarControlSet();
      case HideUntilControlSet.TAG:
        return new HideUntilControlSet();
      case ReminderControlSet.TAG:
        return new ReminderControlSet();
      case FilesControlSet.TAG:
        return new FilesControlSet();
      case TimerControlSet.TAG:
        return new TimerControlSet();
      case TagsControlSet.TAG:
        return new TagsControlSet();
      case RepeatControlSet.TAG:
        return new RepeatControlSet();
      case CommentBarFragment.TAG:
        return new CommentBarFragment();
      case RemoteListFragment.TAG:
        return syncAdapters.isSyncEnabled() ? new RemoteListFragment() : null;
      default:
        throw new RuntimeException("Unsupported fragment");
    }
  }
}
