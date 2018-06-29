/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import static org.tasks.date.DateTimeUtils.newDateTime;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.notes.CommentsController;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.timers.TimerPlugin;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.utility.Flags;
import java.util.List;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.data.UserActivity;
import org.tasks.data.UserActivityDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.fragments.TaskEditControlSetFragmentManager;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.injection.InjectingFragment;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.Preferences;
import org.tasks.ui.MenuColorizer;
import org.tasks.ui.TaskEditControlFragment;

public final class TaskEditFragment extends InjectingFragment
    implements Toolbar.OnMenuItemClickListener {

  public static final String TAG_TASKEDIT_FRAGMENT = "taskedit_fragment";
  private static final String EXTRA_TASK = "extra_task";
  @Inject TaskDao taskDao;
  @Inject UserActivityDao userActivityDao;
  @Inject TaskDeleter taskDeleter;
  @Inject NotificationManager notificationManager;
  @Inject DialogBuilder dialogBuilder;
  @Inject @ForActivity Context context;
  @Inject TaskEditControlSetFragmentManager taskEditControlSetFragmentManager;
  @Inject CommentsController commentsController;
  @Inject Preferences preferences;
  @Inject Tracker tracker;
  @Inject TimerPlugin timerPlugin;
  @Inject LocalBroadcastManager localBroadcastManager;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.comments)
  LinearLayout comments;

  @BindView(R.id.control_sets)
  LinearLayout controlSets;

  Task model = null;
  private TaskEditFragmentCallbackHandler callback;

  public static TaskEditFragment newTaskEditFragment(Task task) {
    TaskEditFragment taskEditFragment = new TaskEditFragment();
    Bundle arguments = new Bundle();
    arguments.putParcelable(EXTRA_TASK, task);
    taskEditFragment.setArguments(arguments);
    return taskEditFragment;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    callback = (TaskEditFragmentCallbackHandler) activity;
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_task_edit, container, false);
    ButterKnife.bind(this, view);

    Bundle arguments = getArguments();
    model = arguments.getParcelable(EXTRA_TASK);

    final boolean backButtonSavesTask = preferences.backButtonSavesTask();
    toolbar.setNavigationIcon(
        ContextCompat.getDrawable(
            context, backButtonSavesTask ? R.drawable.ic_close_24dp : R.drawable.ic_save_24dp));
    toolbar.setNavigationOnClickListener(
        v -> {
          if (backButtonSavesTask) {
            discardButtonClick();
          } else {
            save();
          }
        });
    if (!model.isNew()) {
      toolbar.inflateMenu(R.menu.menu_task_edit_fragment);
    }
    toolbar.setOnMenuItemClickListener(this);
    MenuColorizer.colorToolbar(context, toolbar);

    if (!model.isNew()) {
      notificationManager.cancel(model.getId());
    }

    commentsController.initialize(model, comments);
    commentsController.reloadView();

    FragmentManager fragmentManager = getChildFragmentManager();
    List<TaskEditControlFragment> taskEditControlFragments =
        taskEditControlSetFragmentManager.getOrCreateFragments(this, model);

    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    for (int i = 0; i < taskEditControlFragments.size(); i++) {
      TaskEditControlFragment taskEditControlFragment = taskEditControlFragments.get(i);
      String tag = getString(taskEditControlFragment.controlId());
      fragmentTransaction.replace(
          TaskEditControlSetFragmentManager.TASK_EDIT_CONTROL_FRAGMENT_ROWS[i],
          taskEditControlFragment,
          tag);
    }
    fragmentTransaction.commit();

    for (int i = taskEditControlFragments.size() - 2; i > 1; i--) {
      controlSets.addView(inflater.inflate(R.layout.task_edit_row_divider, controlSets, false), i);
    }

    return view;
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    AndroidUtilities.hideKeyboard(getActivity());

    switch (item.getItemId()) {
      case R.id.menu_delete:
        deleteButtonClick();
        return true;
    }

    return false;
  }

  public Task stopTimer() {
    timerPlugin.stopTimer(model);
    String elapsedTime = DateUtils.formatElapsedTime(model.getElapsedSeconds());
    addComment(
        String.format(
            "%s %s\n%s %s", // $NON-NLS-1$
            getString(R.string.TEA_timer_comment_stopped),
            DateUtilities.getTimeString(getActivity(), newDateTime()),
            getString(R.string.TEA_timer_comment_spent),
            elapsedTime),
        null);
    return model;
  }

  public Task startTimer() {
    timerPlugin.startTimer(model);
    addComment(
        String.format(
            "%s %s",
            getString(R.string.TEA_timer_comment_started),
            DateUtilities.getTimeString(getActivity(), newDateTime())),
        null);
    return model;
  }

  /** Save task model from values in UI components */
  public void save() {
    List<TaskEditControlFragment> fragments =
        taskEditControlSetFragmentManager.getFragmentsInPersistOrder(getChildFragmentManager());
    if (hasChanges(fragments)) {
      boolean isNewTask = model.isNew();
      if (isNewTask) {
        taskDao.createNew(model);
      }
      for (TaskEditControlFragment fragment : fragments) {
        fragment.apply(model);
      }
      taskDao.save(model, null);

      if (Flags.checkAndClear(Flags.TAGS_CHANGED)) {
        localBroadcastManager.broadcastRefreshList();
      }

      if (isNewTask) {
        ((MainActivity) getActivity()).getTaskListFragment().onTaskCreated(model.getUuid());
      } else {
        ((MainActivity) getActivity()).getTaskListFragment().onTaskSaved();
      }
      callback.taskEditFinished();
    } else {
      discard();
    }
  }

  /*
   * ======================================================================
   * =============================================== model reading / saving
   * ======================================================================
   */

  private EditTitleControlSet getEditTitleControlSet() {
    return getFragment(EditTitleControlSet.TAG);
  }

  private RepeatControlSet getRepeatControlSet() {
    return getFragment(RepeatControlSet.TAG);
  }

  private FilesControlSet getFilesControlSet() {
    return getFragment(FilesControlSet.TAG);
  }

  @SuppressWarnings("unchecked")
  private <T extends TaskEditControlFragment> T getFragment(int tag) {
    return (T) getChildFragmentManager().findFragmentByTag(getString(tag));
  }

  private boolean hasChanges(List<TaskEditControlFragment> fragments) {
    try {
      for (TaskEditControlFragment fragment : fragments) {
        if (fragment.hasChanges(model)) {
          return true;
        }
      }
    } catch (Exception e) {
      tracker.reportException(e);
    }
    return false;
  }

  /*
   * ======================================================================
   * ======================================================= event handlers
   * ======================================================================
   */

  public void discardButtonClick() {
    if (hasChanges(
        taskEditControlSetFragmentManager.getFragmentsInPersistOrder(getChildFragmentManager()))) {
      dialogBuilder
          .newMessageDialog(R.string.discard_confirmation)
          .setPositiveButton(R.string.keep_editing, null)
          .setNegativeButton(R.string.discard, (dialog, which) -> discard())
          .show();
    } else {
      discard();
    }
  }

  public void discard() {
    if (model != null && model.isNew()) {
      timerPlugin.stopTimer(model);
    }

    callback.taskEditFinished();
  }

  private void deleteButtonClick() {
    dialogBuilder
        .newMessageDialog(R.string.DLG_delete_this_task_question)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              taskDeleter.markDeleted(model);
              callback.taskEditFinished();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  public void onPriorityChange(int priority) {
    getEditTitleControlSet().setPriority(priority);
  }

  /*
   * ======================================================================
   * ========================================== UI component helper classes
   * ======================================================================
   */

  public void onRepeatChanged(boolean repeat) {
    getEditTitleControlSet().repeatChanged(repeat);
  }

  public void onDueDateChanged(long dueDate) {
    RepeatControlSet repeatControlSet = getRepeatControlSet();
    if (repeatControlSet != null) {
      repeatControlSet.onDueDateChanged(dueDate);
    }
  }

  public void addComment(String message, String picture) {
    UserActivity userActivity = new UserActivity();
    userActivity.setMessage(message);
    userActivity.setTargetId(model.getUuid());
    userActivity.setCreated(DateUtilities.now());
    if (picture != null) {
      userActivity.setPicture(picture);
    }
    userActivityDao.createNew(userActivity);
    commentsController.reloadView();
  }

  public interface TaskEditFragmentCallbackHandler {

    void taskEditFinished();
  }
}
