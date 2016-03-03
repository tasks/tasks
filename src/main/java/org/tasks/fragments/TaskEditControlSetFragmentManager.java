package org.tasks.fragments;

import android.app.Activity;
import android.app.FragmentManager;
import android.view.ContextMenu;

import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.HideUntilControlSet;
import com.todoroo.astrid.ui.ReminderControlSet;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CalendarControlSet;
import org.tasks.ui.DeadlineControlSet;
import org.tasks.ui.DescriptionControlSet;
import org.tasks.ui.PriorityControlSet;
import org.tasks.ui.TaskEditControlFragment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class TaskEditControlSetFragmentManager {

    public static final int[] TASK_EDIT_CONTROL_FRAGMENT_ROWS = new int[] {
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
    };

    public static final int[] TASK_EDIT_DIVIDER_ROWS = new int[] {
            R.id.row_divider_10,
            R.id.row_divider_9,
            R.id.row_divider_8,
            R.id.row_divider_7,
            R.id.row_divider_6,
            R.id.row_divider_5,
            R.id.row_divider_4,
            R.id.row_divider_3,
            R.id.row_divider_2,
            R.id.row_divider_1
    };

    private static final int[] TASK_EDIT_CONTROL_SET_FRAGMENTS = new int[] {
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
            CommentBarFragment.TAG
    };

    static {
        if (BuildConfig.DEBUG && TASK_EDIT_CONTROL_FRAGMENT_ROWS.length != TASK_EDIT_CONTROL_SET_FRAGMENTS.length) {
            throw new AssertionError();
        }
    }

    private final Map<String, Integer> controlSetFragments = new LinkedHashMap<>();
    private final List<String> displayOrder;
    private final FragmentManager fragmentManager;
    private int numRows;

    @Inject
    public TaskEditControlSetFragmentManager(Activity activity, Preferences preferences) {
        displayOrder = BeastModePreferences.constructOrderedControlList(preferences, activity);
        displayOrder.add(0, activity.getString(EditTitleControlSet.TAG));
        displayOrder.add(1, activity.getString(CommentBarFragment.TAG));
        fragmentManager = activity.getFragmentManager();
        String hideAlwaysTrigger = activity.getString(R.string.TEA_ctrl_hide_section_pref);
        for (numRows = 0 ; numRows < displayOrder.size() ; numRows++) {
            if (displayOrder.get(numRows).equals(hideAlwaysTrigger)) {
                break;
            }
        }

        for (int resId : TASK_EDIT_CONTROL_SET_FRAGMENTS) {
            controlSetFragments.put(activity.getString(resId), resId);
        }
    }

    public int getNumRows() {
        return numRows;
    }

    public List<TaskEditControlFragment> createNewFragments(boolean isNewTask, Task task) {
        List<TaskEditControlFragment> taskEditControlFragments = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            String item = displayOrder.get(i);
            Integer resId = controlSetFragments.get(item);
            if (resId == null) {
                Timber.e("Unknown task edit control %s", item);
                continue;
            }

            TaskEditControlFragment fragment = createFragment(resId);
            fragment.initialize(isNewTask, task);
            taskEditControlFragments.add(fragment);
        }
        return taskEditControlFragments;
    }

    public List<TaskEditControlFragment> getFragmentsInDisplayOrder() {
        return getFragments(displayOrder);
    }

    public List<TaskEditControlFragment> getFragmentsInPersistOrder() {
        return getFragments(controlSetFragments.keySet());
    }

    private List<TaskEditControlFragment> getFragments(Iterable<String> tags) {
        List<TaskEditControlFragment> fragments = new ArrayList<>();
        for (String tag : tags) {
            TaskEditControlFragment fragment = (TaskEditControlFragment) fragmentManager.findFragmentByTag(tag);
            if (fragment != null) {
                fragments.add(fragment);
            }
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
            default:
                throw new RuntimeException("Unsupported fragment");
        }
    }
}
