package org.tasks.ui;

import static android.app.Activity.RESULT_OK;
import static org.tasks.activities.RemoteListPicker.newRemoteListSupportPicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import butterknife.BindView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.service.TaskMover;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.RemoteListPicker;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.DefaultFilterProvider;

public class RemoteListFragment extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_google_task_list;
  private static final String FRAG_TAG_GOOGLE_TASK_LIST_SELECTION =
      "frag_tag_google_task_list_selection";
  private static final String EXTRA_ORIGINAL_LIST = "extra_original_list";
  private static final String EXTRA_SELECTED_LIST = "extra_selected_list";
  private static final int REQUEST_CODE_SELECT_LIST = 10101;

  @BindView(R.id.dont_sync)
  TextView textView;

  @BindView(R.id.chip_group)
  ChipGroup chipGroup;

  @Inject GtasksListService gtasksListService;
  @Inject GoogleTaskDao googleTaskDao;
  @Inject CaldavDao caldavDao;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject TaskMover taskMover;
  @Inject ChipProvider chipProvider;

  @Nullable private Filter originalList;
  @Nullable private Filter selectedList;
  private OnListChanged callback;

  public interface OnListChanged {
    void onListchanged(@Nullable Filter filter);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    callback = (OnListChanged) activity;
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    if (savedInstanceState != null) {
      originalList = savedInstanceState.getParcelable(EXTRA_ORIGINAL_LIST);
      setSelected(savedInstanceState.getParcelable(EXTRA_SELECTED_LIST));
    } else {
      if (task.isNew()) {
        if (task.hasTransitory(GoogleTask.KEY)) {
          GoogleTaskList googleTaskList =
              gtasksListService.getList(task.getTransitory(GoogleTask.KEY));
          if (googleTaskList != null) {
            originalList = new GtasksFilter(googleTaskList);
          }
        } else if (task.hasTransitory(CaldavTask.KEY)) {
          CaldavCalendar caldav = caldavDao.getCalendarByUuid(task.getTransitory(CaldavTask.KEY));
          if (caldav != null) {
            originalList = new CaldavFilter(caldav);
          }
        } else {
          originalList = defaultFilterProvider.getDefaultRemoteList();
        }
      } else {
        GoogleTask googleTask = googleTaskDao.getByTaskId(task.getId());
        CaldavTask caldavTask = caldavDao.getTask(task.getId());
        if (googleTask != null) {
          GoogleTaskList googleTaskList = gtasksListService.getList(googleTask.getListId());
          if (googleTaskList != null) {
            originalList = new GtasksFilter(googleTaskList);
          }
        } else if (caldavTask != null) {
          CaldavCalendar calendarByUuid = caldavDao.getCalendarByUuid(caldavTask.getCalendar());
          if (calendarByUuid != null) {
            originalList = new CaldavFilter(calendarByUuid);
          }
        }
      }

      setSelected(originalList);
    }

    return view;
  }

  private void setSelected(@Nullable Filter filter) {
    selectedList = filter;
    refreshView();
    callback.onListchanged(filter);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    if (originalList != null) {
      outState.putParcelable(EXTRA_ORIGINAL_LIST, originalList);
    }
    if (selectedList != null) {
      outState.putParcelable(EXTRA_SELECTED_LIST, selectedList);
    }
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_remote_list;
  }

  @Override
  protected int getIcon() {
    return R.drawable.ic_outline_cloud_24px;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @Override
  protected void onRowClick() {
    openPicker();
  }

  @Override
  protected boolean isClickable() {
    return true;
  }

  private void openPicker() {
    newRemoteListSupportPicker(selectedList, this, REQUEST_CODE_SELECT_LIST)
        .show(getFragmentManager(), FRAG_TAG_GOOGLE_TASK_LIST_SELECTION);
  }

  @Override
  public boolean requiresId() {
    return true;
  }

  @Override
  public void apply(Task task) {
    if (isNew() || hasChanges()) {
      task.setParent(0);
      task.setParentUuid(null);
      taskMover.move(ImmutableList.of(task.getId()), selectedList);
    }
  }

  @Override
  public boolean hasChanges(Task original) {
    return hasChanges();
  }

  private boolean hasChanges() {
    return !Objects.equal(selectedList, originalList);
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_SELECT_LIST) {
      if (resultCode == RESULT_OK) {
        setList(data.getParcelableExtra(RemoteListPicker.EXTRA_SELECTED_FILTER));
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void setList(Filter list) {
    if (list == null) {
      setSelected(null);
    } else if (list instanceof GtasksFilter || list instanceof CaldavFilter) {
      setSelected(list);
    } else {
      throw new RuntimeException("Unhandled filter type");
    }
  }

  private void refreshView() {
    if (selectedList == null) {
      textView.setVisibility(View.VISIBLE);
      chipGroup.setVisibility(View.GONE);
    } else {
      textView.setVisibility(View.GONE);
      chipGroup.setVisibility(View.VISIBLE);
      chipGroup.removeAllViews();
      Chip chip =
          chipProvider.newChip(selectedList, R.drawable.ic_outline_cloud_24px, true, true);
      chip.setCloseIconVisible(true);
      chip.setOnClickListener(v -> openPicker());
      chip.setOnCloseIconClickListener(v -> setSelected(null));
      chipGroup.addView(chip);
    }
  }
}
