/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.google.common.base.Strings;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.FragmentComponent;
import org.tasks.ui.CheckBoxes;
import org.tasks.ui.TaskEditControlFragment;

/**
 * Control set for mapping a Property to an EditText
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class EditTitleControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_title_pref;

  private static final String EXTRA_COMPLETE = "extra_complete";
  private static final String EXTRA_TITLE = "extra_title";
  private static final String EXTRA_REPEATING = "extra_repeating";
  private static final String EXTRA_PRIORITY = "extra_priority";

  @Inject TaskDao taskDao;
  @Inject CheckBoxes checkBoxes;

  @BindView(R.id.title)
  EditText editText;

  @BindView(R.id.completeBox)
  CheckableImageView completeBox;

  private boolean showKeyboard;
  private boolean isComplete;
  private boolean isRepeating;
  private int importanceValue;
  private String title;

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(getLayout(), null);
    ButterKnife.bind(this, view);
    if (savedInstanceState == null) {
      isComplete = task.isCompleted();
      title = task.getTitle();
      isRepeating = !TextUtils.isEmpty(task.getRecurrence());
      importanceValue = task.getPriority();
      showKeyboard = task.isNew() && Strings.isNullOrEmpty(title);
    } else {
      isComplete = savedInstanceState.getBoolean(EXTRA_COMPLETE);
      title = savedInstanceState.getString(EXTRA_TITLE);
      isRepeating = savedInstanceState.getBoolean(EXTRA_REPEATING);
      importanceValue = savedInstanceState.getInt(EXTRA_PRIORITY);
    }
    completeBox.setChecked(isComplete);
    editText.setTextKeepState(title);
    editText.setHorizontallyScrolling(false);
    editText.setLines(1);
    editText.setMaxLines(Integer.MAX_VALUE);
    updateCompleteBox();
    return view;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putBoolean(EXTRA_COMPLETE, isComplete);
    outState.putString(EXTRA_TITLE, title);
    outState.putBoolean(EXTRA_REPEATING, isRepeating);
    outState.putInt(EXTRA_PRIORITY, importanceValue);
  }

  @OnClick(R.id.completeBox)
  void toggleComplete(View view) {
    updateCompleteBox();
  }

  @Override
  public void onResume() {
    super.onResume();

    if (showKeyboard) {
      editText.requestFocus();
      InputMethodManager imm =
          (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }
  }

  @OnTextChanged(R.id.title)
  void onTextChanged(CharSequence text) {
    this.title = text.toString().trim();
  }

  public void setPriority(int priority) {
    importanceValue = priority;
    updateCompleteBox();
  }

  public void repeatChanged(boolean repeat) {
    isRepeating = repeat;
    updateCompleteBox();
  }

  private void updateCompleteBox() {
    isComplete = completeBox.isChecked();

    if (isComplete) {
      completeBox.setImageDrawable(checkBoxes.getCompletedCheckbox(importanceValue));
    } else if (isRepeating) {
      completeBox.setImageDrawable(checkBoxes.getRepeatingCheckBox(importanceValue));
    } else {
      completeBox.setImageDrawable(checkBoxes.getCheckBox(importanceValue));
    }

    if (isComplete) {
      editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    } else {
      editText.setPaintFlags(editText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_title;
  }

  @Override
  public int getIcon() {
    return -1;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @Override
  public boolean hasChanges(Task original) {
    return !title.equals(original.getTitle())
        || isComplete != original.isCompleted()
        || (original.isNew() && !Strings.isNullOrEmpty(title));
  }

  @Override
  public void apply(Task task) {
    task.setTitle(Strings.isNullOrEmpty(title) ? getString(R.string.no_title) : title);
    if (isComplete != task.isCompleted()) {
      taskDao.setComplete(task, isComplete);
    }
  }
}
