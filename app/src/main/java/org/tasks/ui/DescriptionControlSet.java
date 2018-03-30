package org.tasks.ui;

import static com.google.common.base.Strings.isNullOrEmpty;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.OnTextChanged;
import com.todoroo.astrid.data.Task;
import org.tasks.R;
import org.tasks.injection.FragmentComponent;

public class DescriptionControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_notes_pref;
  private static final String EXTRA_DESCRIPTION = "extra_description";

  @BindView(R.id.notes)
  EditText editText;

  private String description;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    if (savedInstanceState == null) {
      description = task.getNotes();
    } else {
      description = savedInstanceState.getString(EXTRA_DESCRIPTION);
    }
    if (!isNullOrEmpty(description)) {
      editText.setTextKeepState(description);
    }
    return view;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(EXTRA_DESCRIPTION, description);
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_description;
  }

  @Override
  protected int getIcon() {
    return R.drawable.ic_event_note_24dp;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @OnTextChanged(R.id.notes)
  void textChanged(CharSequence text) {
    description = text.toString().trim();
  }

  @Override
  public void apply(Task task) {
    task.setNotes(description);
  }

  @Override
  public boolean hasChanges(Task original) {
    return !(isNullOrEmpty(description)
        ? isNullOrEmpty(original.getNotes())
        : description.equals(original.getNotes()));
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }
}
