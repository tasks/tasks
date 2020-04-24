package org.tasks.ui;

import static org.tasks.date.DateTimeUtils.newDateTime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DateTimePicker;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.locale.Locale;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;
import org.threeten.bp.format.FormatStyle;

public class DeadlineControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_when_pref;

  private static final int REQUEST_DATE = 504;
  private static final String EXTRA_DATE = "extra_date";
  private static final String FRAG_TAG_DATE_PICKER = "frag_tag_date_picker";

  @Inject @ForActivity Context context;
  @Inject Locale locale;
  @Inject Preferences preferences;

  @BindView(R.id.due_date)
  TextView dueDate;

  private DueDateChangeListener callback;
  private long date = 0;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    callback = (DueDateChangeListener) activity;
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(
      final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    if (savedInstanceState == null) {
      date = task.getDueDate();
    } else {
      date = savedInstanceState.getLong(EXTRA_DATE);
    }

    refreshDisplayView();

    return view;
  }

  @Override
  protected void onRowClick() {
    FragmentManager fragmentManager = getParentFragmentManager();
    if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
      DateTimePicker.Companion.newDateTimePicker(
          this,
          REQUEST_DATE,
          getDueDateTime(),
          preferences.getBoolean(R.string.p_auto_dismiss_datetime_edit_screen, false))
          .show(fragmentManager, FRAG_TAG_DATE_PICKER);
    }
  }

  @Override
  protected boolean isClickable() {
    return true;
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_deadline;
  }

  @Override
  protected int getIcon() {
    return R.drawable.ic_outline_schedule_24px;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @Override
  public boolean hasChanges(Task original) {
    return original.getDueDate() != getDueDateTime();
  }

  @Override
  public void apply(Task task) {
    long dueDate = getDueDateTime();
    if (dueDate != task.getDueDate()) {
      task.setReminderSnooze(0L);
    }
    task.setDueDate(dueDate);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_DATE) {
      if (resultCode == Activity.RESULT_OK) {
        long timestamp = data.getLongExtra(DateTimePicker.EXTRA_TIMESTAMP, 0L);
        DateTime dateTime = new DateTime(timestamp);
        date = dateTime.getMillis();
        callback.dueDateChanged(getDueDateTime());
      }
      refreshDisplayView();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private long getDueDateTime() {
    return date == 0
        ? 0
        : Task.createDueDate(
            Task.hasDueTime(date) ? Task.URGENCY_SPECIFIC_DAY_TIME : Task.URGENCY_SPECIFIC_DAY,
            date);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putLong(EXTRA_DATE, date);
  }

  private void refreshDisplayView() {
    if (date == 0) {
      dueDate.setText("");
      setTextColor(false);
    } else {
      dueDate.setText(
          DateUtilities.getRelativeDateTime(context, date, locale.getLocale(), FormatStyle.FULL));
      setTextColor(
          Task.hasDueTime(date)
              ? newDateTime(date).isBeforeNow()
              : newDateTime(date).endOfDay().isBeforeNow());
    }
  }

  private void setTextColor(boolean overdue) {
    dueDate.setTextColor(
        context.getColor(overdue ? R.color.overdue : R.color.text_primary));
  }

  public interface DueDateChangeListener {

    void dueDateChanged(long dateTime);
  }
}
