/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.activity;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastQ;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static org.tasks.Strings.isNullOrEmpty;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.files.FileHelper.copyToUri;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.appbar.AppBarLayout;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.notes.CommentsController;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.timers.TimerPlugin;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Firebase;
import org.tasks.data.UserActivity;
import org.tasks.data.UserActivityDao;
import org.tasks.databinding.FragmentTaskEditBinding;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.Linkify;
import org.tasks.fragments.TaskEditControlSetFragmentManager;
import org.tasks.injection.ActivityContext;
import org.tasks.injection.FragmentComponent;
import org.tasks.injection.InjectingFragment;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.SubtaskControlSet;
import org.tasks.ui.TaskEditControlFragment;

public final class TaskEditFragment extends InjectingFragment
    implements Toolbar.OnMenuItemClickListener {

  static final String TAG_TASKEDIT_FRAGMENT = "taskedit_fragment";
  private static final String EXTRA_TASK = "extra_task";
  private static final String EXTRA_THEME = "extra_theme";
  private static final String EXTRA_COMPLETED = "extra_completed";

  @Inject TaskDao taskDao;
  @Inject UserActivityDao userActivityDao;
  @Inject TaskDeleter taskDeleter;
  @Inject NotificationManager notificationManager;
  @Inject DialogBuilder dialogBuilder;
  @Inject @ActivityContext Context context;
  @Inject TaskEditControlSetFragmentManager taskEditControlSetFragmentManager;
  @Inject CommentsController commentsController;
  @Inject Preferences preferences;
  @Inject Firebase firebase;
  @Inject TimerPlugin timerPlugin;
  @Inject Linkify linkify;

  Task model = null;
  private TaskEditFragmentCallbackHandler callback;
  private boolean showKeyboard;
  private FragmentTaskEditBinding binding;
  private boolean completed;

  static TaskEditFragment newTaskEditFragment(Task task, ThemeColor themeColor) {
    TaskEditFragment taskEditFragment = new TaskEditFragment();
    Bundle arguments = new Bundle();
    arguments.putParcelable(EXTRA_TASK, task);
    arguments.putParcelable(EXTRA_THEME, themeColor);
    taskEditFragment.setArguments(arguments);
    return taskEditFragment;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    callback = (TaskEditFragmentCallbackHandler) activity;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putBoolean(EXTRA_COMPLETED, completed);
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentTaskEditBinding.inflate(inflater);
    View view = binding.getRoot();

    Bundle arguments = getArguments();
    model = arguments.getParcelable(EXTRA_TASK);
    ThemeColor themeColor = arguments.getParcelable(EXTRA_THEME);

    Toolbar toolbar = binding.toolbar;
    toolbar.setNavigationIcon(context.getDrawable(R.drawable.ic_outline_save_24px));
    toolbar.setNavigationOnClickListener(v -> save());

    boolean backButtonSavesTask = preferences.backButtonSavesTask();
    toolbar.inflateMenu(R.menu.menu_task_edit_fragment);
    Menu menu = toolbar.getMenu();
    MenuItem delete = menu.findItem(R.id.menu_delete);
    delete.setVisible(!model.isNew());
    delete.setShowAsAction(
        backButtonSavesTask ? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_IF_ROOM);
    MenuItem discard = menu.findItem(R.id.menu_discard);
    discard.setVisible(backButtonSavesTask);
    discard.setShowAsAction(
        model.isNew() ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER);

    if (savedInstanceState == null) {
      showKeyboard = model.isNew() && isNullOrEmpty(model.getTitle());
      completed = model.isCompleted();
    } else {
      completed = savedInstanceState.getBoolean(EXTRA_COMPLETED);
    }

    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) binding.appbarlayout.getLayoutParams();
    params.setBehavior(new AppBarLayout.Behavior());
    AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
    behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
      @Override
      public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
        return false;
      }
    });
    toolbar.setOnMenuItemClickListener(this);
    themeColor.apply(binding.collapsingtoolbarlayout, toolbar);
    EditText title = binding.title;
    title.setText(model.getTitle());
    title.setHorizontallyScrolling(false);
    title.setTextColor(themeColor.getColorOnPrimary());
    title.setHintTextColor(themeColor.getHintOnPrimary());
    title.setMaxLines(5);
    if (model.isNew() || preferences.getBoolean(R.string.p_hide_check_button, false)) {
      binding.fab.setVisibility(View.INVISIBLE);
    } else if (completed) {
      title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
      binding.fab.setImageResource(R.drawable.ic_outline_check_box_outline_blank_24px);
    }
    binding.fab.setOnClickListener(v -> {
      if (completed) {
        completed = false;
        title.setPaintFlags(title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        binding.fab.setImageResource(R.drawable.ic_outline_check_box_24px);
      } else {
        completed = true;
        save();
      }
    });

    if (atLeastQ()) {
      title.setVerticalScrollbarThumbDrawable(new ColorDrawable(themeColor.getHintOnPrimary()));
    }
    binding.appbarlayout.addOnOffsetChangedListener(
        (appBarLayout, verticalOffset) -> {
          if (verticalOffset == 0) {
            title.setVisibility(View.VISIBLE);
            binding.collapsingtoolbarlayout.setTitleEnabled(false);
          } else if (Math.abs(verticalOffset) < appBarLayout.getTotalScrollRange()) {
            title.setVisibility(View.INVISIBLE);
            binding.collapsingtoolbarlayout.setTitle(title.getText());
            binding.collapsingtoolbarlayout.setTitleEnabled(true);
          }
        });

    if (!model.isNew()) {
      notificationManager.cancel(model.getId());

      if (preferences.getBoolean(R.string.p_linkify_task_edit, false)) {
        linkify.linkify(title);
      }
    }

    commentsController.initialize(model, binding.comments);
    commentsController.reloadView();

    FragmentManager fragmentManager = getChildFragmentManager();
    List<TaskEditControlFragment> taskEditControlFragments =
        taskEditControlSetFragmentManager.getOrCreateFragments(this, model);
    int visibleSize = taskEditControlSetFragmentManager.getVisibleSize();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    for (int i = 0; i < taskEditControlFragments.size(); i++) {
      TaskEditControlFragment taskEditControlFragment = taskEditControlFragments.get(i);
      String tag = getString(taskEditControlFragment.controlId());
      fragmentTransaction.replace(
          TaskEditControlSetFragmentManager.TASK_EDIT_CONTROL_FRAGMENT_ROWS[i],
          taskEditControlFragment,
          tag);
      if (i >= visibleSize) {
        fragmentTransaction.hide(taskEditControlFragment);
      }
    }
    fragmentTransaction.commit();

    for (int i = visibleSize - 1; i > 0; i--) {
      binding.controlSets.addView(inflater.inflate(R.layout.task_edit_row_divider, binding.controlSets, false), i);
    }

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();

    if (showKeyboard) {
      binding.title.requestFocus();
      InputMethodManager imm =
          (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(binding.title, InputMethodManager.SHOW_IMPLICIT);
    }
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    AndroidUtilities.hideKeyboard(getActivity());

    if (item.getItemId() == R.id.menu_delete) {
      deleteButtonClick();
      return true;
    } else if (item.getItemId() == R.id.menu_discard) {
      discardButtonClick();
      return true;
    }

    return false;
  }

  Task stopTimer() {
    timerPlugin.stopTimer(model);
    String elapsedTime = DateUtils.formatElapsedTime(model.getElapsedSeconds());
    addComment(
        String.format(
            "%s %s\n%s %s", // $NON-NLS-1$
            getString(R.string.TEA_timer_comment_stopped),
            DateUtilities.getTimeString(context, newDateTime()),
            getString(R.string.TEA_timer_comment_spent),
            elapsedTime),
        null);
    return model;
  }

  Task startTimer() {
    timerPlugin.startTimer(model);
    addComment(
        String.format(
            "%s %s",
            getString(R.string.TEA_timer_comment_started),
            DateUtilities.getTimeString(context, newDateTime())),
        null);
    return model;
  }

  /** Save task model from values in UI components */
  public void save() {
    List<TaskEditControlFragment> fragments =
        taskEditControlSetFragmentManager.getFragmentsInPersistOrder(getChildFragmentManager());
    if (hasChanges(fragments)) {
      boolean isNewTask = model.isNew();
      TaskListFragment taskListFragment = ((MainActivity) getActivity()).getTaskListFragment();
      String title = getTitle();
      model.setTitle(isNullOrEmpty(title) ? getString(R.string.no_title) : title);
      if (completed != model.isCompleted()) {
        model.setCompletionDate(completed ? now() : 0);
      }
      for (TaskEditControlFragment fragment :
          filter(fragments, not(TaskEditControlFragment::requiresId))) {
        fragment.apply(model);
      }

      Completable.fromAction(
              () -> {
                assertNotMainThread();

                if (isNewTask) {
                  taskDao.createNew(model);
                }

                for (TaskEditControlFragment fragment :
                    filter(fragments, TaskEditControlFragment::requiresId)) {
                  fragment.apply(model);
                }

                taskDao.save(model, null);

                if (isNewTask) {
                  taskListFragment.onTaskCreated(model.getUuid());
                  if (!isNullOrEmpty(model.getCalendarURI())) {
                    taskListFragment.makeSnackbar(R.string.calendar_event_created, model.getTitle())
                        .setAction(R.string.action_open, v -> {
                          String uri = model.getCalendarURI();
                          Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                          taskListFragment.startActivity(intent);
                        })
                        .show();
                  }
                }
              })
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe();
      callback.removeTaskEditFragment();
    } else {
      discard();
    }
  }

  /*
   * ======================================================================
   * =============================================== model reading / saving
   * ======================================================================
   */

  private RepeatControlSet getRepeatControlSet() {
    return getFragment(RepeatControlSet.TAG);
  }

  private SubtaskControlSet getSubtaskControlSet() {
    return getFragment(SubtaskControlSet.TAG);
  }

  @SuppressWarnings("unchecked")
  private <T extends TaskEditControlFragment> T getFragment(int tag) {
    return (T) getChildFragmentManager().findFragmentByTag(getString(tag));
  }

  private String getTitle() {
    return binding.title.getText().toString().trim();
  }

  private boolean hasChanges(List<TaskEditControlFragment> fragments) {
    String newTitle = getTitle();
    if (!newTitle.equals(model.getTitle())
        || (!model.isNew() && completed != model.isCompleted())
        || (model.isNew() && !isNullOrEmpty(newTitle))) {
      return true;
    }

    try {
      for (TaskEditControlFragment fragment : fragments) {
        if (fragment.hasChanges(model)) {
          return true;
        }
      }
    } catch (Exception e) {
      firebase.reportException(e);
    }
    return false;
  }

  /*
   * ======================================================================
   * ======================================================= event handlers
   * ======================================================================
   */

  void discardButtonClick() {
    if (hasChanges(
        taskEditControlSetFragmentManager.getFragmentsInPersistOrder(getChildFragmentManager()))) {
      dialogBuilder
          .newDialog(R.string.discard_confirmation)
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

    callback.removeTaskEditFragment();
  }

  private void deleteButtonClick() {
    dialogBuilder
        .newDialog(R.string.DLG_delete_this_task_question)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              taskDeleter.markDeleted(model);
              callback.removeTaskEditFragment();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  /*
   * ======================================================================
   * ========================================== UI component helper classes
   * ======================================================================
   */

  void onDueDateChanged(long dueDate) {
    RepeatControlSet repeatControlSet = getRepeatControlSet();
    if (repeatControlSet != null) {
      repeatControlSet.onDueDateChanged(dueDate);
    }
  }

  void onRemoteListChanged(@Nullable Filter filter) {
    SubtaskControlSet subtaskControlSet = getSubtaskControlSet();
    if (subtaskControlSet != null) {
      subtaskControlSet.onRemoteListChanged(filter);
    }
  }

  void addComment(String message, Uri picture) {
    UserActivity userActivity = new UserActivity();
    if (picture != null) {
      Uri output = copyToUri(context, preferences.getAttachmentsDirectory(), picture);
      userActivity.setPicture(output);
    }
    userActivity.setMessage(message);
    userActivity.setTargetId(model.getUuid());
    userActivity.setCreated(now());
    userActivityDao.createNew(userActivity);
    commentsController.reloadView();
  }

  public interface TaskEditFragmentCallbackHandler {

    void removeTaskEditFragment();
  }
}
