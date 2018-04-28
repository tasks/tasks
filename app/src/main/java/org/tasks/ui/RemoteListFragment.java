package org.tasks.ui;

import static android.app.Activity.RESULT_OK;
import static org.tasks.activities.RemoteListSupportPicker.newRemoteListSupportPicker;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.OnClick;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.service.TaskMover;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.RemoteListSupportPicker;
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

  @BindView(R.id.google_task_list)
  TextView textView;

  @Inject GtasksListService gtasksListService;
  @Inject GoogleTaskDao googleTaskDao;
  @Inject CaldavDao caldavDao;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject TaskMover taskMover;

  @Nullable private Filter originalList;
  @Nullable private Filter selectedList;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    if (savedInstanceState != null) {
      originalList = savedInstanceState.getParcelable(EXTRA_ORIGINAL_LIST);
      selectedList = savedInstanceState.getParcelable(EXTRA_SELECTED_LIST);
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

      selectedList = originalList;
    }

    refreshView();
    return view;
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
    return R.drawable.ic_cloud_black_24dp;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @OnClick(R.id.google_task_list)
  void clickGoogleTaskList(View view) {
    newRemoteListSupportPicker(selectedList, this, REQUEST_CODE_SELECT_LIST)
        .show(getFragmentManager(), FRAG_TAG_GOOGLE_TASK_LIST_SELECTION);
  }

  @Override
  public void apply(Task task) {
    taskMover.move(task, selectedList);
  }

  @Override
  public boolean hasChanges(Task original) {
    return selectedList == null ? originalList != null : !selectedList.equals(originalList);
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_SELECT_LIST) {
      if (resultCode == RESULT_OK) {
        setList(data.getParcelableExtra(RemoteListSupportPicker.EXTRA_SELECTED_FILTER));
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void setList(Filter list) {
    if (list == null) {
      this.selectedList = null;
    } else if (list instanceof GtasksFilter || list instanceof CaldavFilter) {
      this.selectedList = list;
    } else {
      throw new RuntimeException("Unhandled filter type");
    }
    refreshView();
  }

  private void refreshView() {
    textView.setText(selectedList == null ? null : selectedList.listingTitle);
  }
}
