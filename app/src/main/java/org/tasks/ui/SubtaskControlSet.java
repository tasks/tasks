package org.tasks.ui;

import static com.todoroo.andlib.utility.DateUtilities.now;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.common.base.Strings;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskCompleter;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.ui.CheckableImageView;
import java.util.ArrayList;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.injection.FragmentComponent;
import org.tasks.locale.Locale;
import org.tasks.preferences.Preferences;
import org.tasks.tasklist.SubtaskViewHolder.Callbacks;
import org.tasks.tasklist.SubtasksRecyclerAdapter;

public class SubtaskControlSet extends TaskEditControlFragment implements Callbacks {

  public static final int TAG = R.string.TEA_ctrl_subtask_pref;
  private static final String EXTRA_NEW_SUBTASKS = "extra_new_subtasks";

  @BindView(R.id.recycler_view)
  RecyclerView recyclerView;

  @BindView(R.id.new_subtasks)
  LinearLayout newSubtaskContainer;

  @Inject Activity activity;
  @Inject TaskCompleter taskCompleter;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject GoogleTaskDao googleTaskDao;
  @Inject Toaster toaster;
  @Inject Preferences preferences;
  @Inject TaskCreator taskCreator;
  @Inject CaldavDao caldavDao;
  @Inject TaskDao taskDao;
  @Inject Locale locale;
  @Inject CheckBoxProvider checkBoxProvider;
  @Inject ChipProvider chipProvider;

  private TaskListViewModel viewModel;
  private final RefreshReceiver refreshReceiver = new RefreshReceiver();
  private Filter remoteList;
  private GoogleTask googleTask;
  private SubtasksRecyclerAdapter recyclerAdapter;

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
    viewModel = ViewModelProviders.of(this).get(TaskListViewModel.class);
    component.inject(viewModel);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelableArrayList(EXTRA_NEW_SUBTASKS, getNewSubtasks());
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = super.onCreateView(inflater, container, savedInstanceState);

    if (savedInstanceState != null) {
      for (Task task : savedInstanceState.<Task>getParcelableArrayList(EXTRA_NEW_SUBTASKS)) {
        addSubtask(task);
      }
    }

    recyclerAdapter = new SubtasksRecyclerAdapter(activity, chipProvider, checkBoxProvider, this);
    if (task.getId() > 0) {
      recyclerAdapter.submitList(viewModel.getValue());
      viewModel.setFilter(new Filter("subtasks", getQueryTemplate(task)), true);
      ((DefaultItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
      recyclerView.setLayoutManager(new LinearLayoutManager(activity));
      recyclerView.setNestedScrollingEnabled(false);
      viewModel.observe(this, recyclerAdapter::submitList);
      recyclerView.setAdapter(recyclerAdapter);
    }
    return view;
  }

  private static QueryTemplate getQueryTemplate(Task task) {
    return new QueryTemplate()
        .join(
            Join.left(
                GoogleTask.TABLE,
                Criterion.and(
                    GoogleTask.PARENT.eq(task.getId()),
                    GoogleTask.TASK.eq(Task.ID),
                    GoogleTask.DELETED.eq(0))))
        .where(
            Criterion.and(
                TaskCriteria.activeAndVisible(),
                Criterion.or(Task.PARENT.eq(task.getId()), GoogleTask.TASK.gt(0))));
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_subtasks;
  }

  @Override
  protected int getIcon() {
    return R.drawable.ic_subdirectory_arrow_right_black_24dp;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @Override
  public boolean requiresId() {
    return true;
  }

  @Override
  public void apply(Task task) {
    for (Task subtask: getNewSubtasks()) {
      if (Strings.isNullOrEmpty(subtask.getTitle())) {
        continue;
      }
      subtask.setCompletionDate(task.getCompletionDate());
      taskDao.createNew(subtask);
      if (remoteList instanceof GtasksFilter) {
        GoogleTask googleTask =
            new GoogleTask(subtask.getId(), ((GtasksFilter) remoteList).getRemoteId());
        googleTask.setParent(task.getId());
        googleTask.setMoved(true);
        googleTaskDao.insertAndShift(googleTask, preferences.addGoogleTasksToTop());
      } else if (remoteList instanceof CaldavFilter) {
        CaldavTask caldavTask =
            new CaldavTask(subtask.getId(), ((CaldavFilter) remoteList).getUuid());
        subtask.setParent(task.getId());
        caldavTask.setRemoteParent(caldavDao.getRemoteIdForTask(task.getId()));
        taskDao.save(subtask);
        caldavDao.insert(caldavTask);
      } else {
        subtask.setParent(task.getId());
        subtask.setParentUuid(task.getUuid());
        taskDao.save(subtask);
      }
    }
  }

  @Override
  public boolean hasChanges(Task original) {
    return !getNewSubtasks().isEmpty();
  }

  private ArrayList<Task> getNewSubtasks() {
    ArrayList<Task> subtasks = new ArrayList<>();
    int children = newSubtaskContainer.getChildCount();
    for (int i = 0 ; i < children ; i++) {
      View view = newSubtaskContainer.getChildAt(i);
      EditText title = view.findViewById(R.id.title);
      CheckableImageView completed = view.findViewById(R.id.completeBox);
      Task subtask = taskCreator.createWithValues(title.getText().toString());
      if (completed.isChecked()) {
        subtask.setCompletionDate(now());
      }
      subtasks.add(subtask);
    }
    return subtasks;
  }

  @Override
  public void onResume() {
    super.onResume();

    localBroadcastManager.registerRefreshReceiver(refreshReceiver);

    googleTask = googleTaskDao.getByTaskId(task.getId());

    updateUI();
  }

  @Override
  public void onPause() {
    super.onPause();

    localBroadcastManager.unregisterReceiver(refreshReceiver);
  }

  @OnClick(R.id.add_subtask)
  void addSubtask() {
    if (isGoogleTaskChild()) {
      toaster.longToast(R.string.subtasks_multilevel_google_task);
      return;
    }
    EditText editText = addSubtask(taskCreator.createWithValues(""));
    editText.requestFocus();
    InputMethodManager imm =
        (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
  }

  private EditText addSubtask(Task task) {
    ViewGroup view =
        (ViewGroup)
            LayoutInflater.from(activity)
                .inflate(R.layout.editable_subtask_adapter_row_body, newSubtaskContainer, false);
    view.findViewById(R.id.clear).setOnClickListener(v -> newSubtaskContainer.removeView(view));
    EditText editText = view.findViewById(R.id.title);
    editText.setTextKeepState(task.getTitle());
    editText.setHorizontallyScrolling(false);
    editText.setLines(1);
    editText.setMaxLines(Integer.MAX_VALUE);
    editText.setFocusable(true);
    editText.setEnabled(true);
    editText.setOnEditorActionListener(
        (arg0, actionId, arg2) -> {
          if (actionId == EditorInfo.IME_ACTION_NEXT) {
            if (editText.getText().length() != 0) {
              addSubtask();
            }
            return true;
          }
          return false;
        });

    CheckableImageView completeBox = view.findViewById(R.id.completeBox);
    completeBox.setChecked(task.isCompleted());
    updateCompleteBox(task, completeBox, editText);
    completeBox.setOnClickListener(v -> updateCompleteBox(task, completeBox, editText));
    newSubtaskContainer.addView(view);
    return editText;
  }

  private void updateCompleteBox(Task task, CheckableImageView completeBox, EditText editText) {
    boolean isComplete = completeBox.isChecked();
    completeBox.setImageDrawable(
        checkBoxProvider.getCheckBox(isComplete, false, task.getPriority()));
    if (isComplete) {
      editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    } else {
      editText.setPaintFlags(editText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }
  }

  private boolean isGoogleTaskChild() {
    return remoteList instanceof GtasksFilter
        && googleTask != null
        && googleTask.getParent() > 0
        && googleTask.getListId().equals(((GtasksFilter) remoteList).getRemoteId());
  }

  private void updateUI() {
    if (isGoogleTaskChild()) {
      recyclerView.setVisibility(View.GONE);
      newSubtaskContainer.setVisibility(View.GONE);
    } else {
      recyclerView.setVisibility(View.VISIBLE);
      newSubtaskContainer.setVisibility(View.VISIBLE);
      recyclerAdapter.setMultiLevelSubtasksEnabled(!(remoteList instanceof GtasksFilter));
      refresh();
    }
  }

  public void onRemoteListChanged(@Nullable Filter filter) {
    this.remoteList = filter;

    if (recyclerView != null) {
      updateUI();
    }
  }

  private void refresh() {
    viewModel.invalidate();
  }

  @Override
  public void openSubtask(Task task) {
    ((MainActivity) getActivity()).getTaskListFragment().onTaskListItemClicked(task);
  }

  @Override
  public void toggleSubtask(long taskId, boolean collapsed) {
    taskDao.setCollapsed(taskId, collapsed);
    localBroadcastManager.broadcastRefresh();
  }

  @Override
  public void complete(Task task, boolean completed) {
    taskCompleter.setComplete(task, completed);
  }

  protected class RefreshReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      refresh();
    }
  }
}
