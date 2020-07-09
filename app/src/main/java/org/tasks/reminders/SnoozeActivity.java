package org.tasks.reminders;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import com.todoroo.astrid.dao.TaskDaoBlocking;
import com.todoroo.astrid.reminders.ReminderService;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.activities.DateAndTimePickerActivity;
import org.tasks.dialogs.MyTimePickerDialog;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.notifications.NotificationManager;
import org.tasks.themes.ThemeAccent;
import org.tasks.time.DateTime;

@AndroidEntryPoint
public class SnoozeActivity extends InjectingAppCompatActivity
    implements SnoozeCallback, DialogInterface.OnCancelListener {

  private static final int FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK;
  public static final String EXTRA_TASK_ID = "id";
  public static final String EXTRA_TASK_IDS = "ids";
  public static final String EXTRA_SNOOZE_TIME = "snooze_time";
  private static final String FRAG_TAG_SNOOZE_DIALOG = "frag_tag_snooze_dialog";
  private static final String EXTRA_PICKING_DATE_TIME = "extra_picking_date_time";
  private static final int REQUEST_DATE_TIME = 10101;
  private final List<Long> taskIds = new ArrayList<>();
  @Inject NotificationManager notificationManager;
  @Inject TaskDaoBlocking taskDao;
  @Inject ReminderService reminderService;
  @Inject ThemeAccent themeAccent;
  private boolean pickingDateTime;

  public static Intent newIntent(Context context, Long id) {
    Intent intent = new Intent(context, SnoozeActivity.class);
    intent.setFlags(FLAGS);
    intent.putExtra(SnoozeActivity.EXTRA_TASK_ID, id);
    return intent;
  }

  public static Intent newIntent(Context context, ArrayList<Long> ids) {
    Intent intent = new Intent(context, SnoozeActivity.class);
    intent.setFlags(FLAGS);
    intent.putExtra(SnoozeActivity.EXTRA_TASK_IDS, ids);
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    themeAccent.applyStyle(getTheme());

    Intent intent = getIntent();

    if (intent.hasExtra(EXTRA_TASK_ID)) {
      taskIds.add(intent.getLongExtra(EXTRA_TASK_ID, -1L));
    } else if (intent.hasExtra(EXTRA_TASK_IDS)) {
      //noinspection unchecked
      taskIds.addAll((ArrayList<Long>) intent.getSerializableExtra(EXTRA_TASK_IDS));
    }

    if (savedInstanceState != null) {
      pickingDateTime = savedInstanceState.getBoolean(EXTRA_PICKING_DATE_TIME, false);
      if (pickingDateTime) {
        return;
      }
    }

    if (intent.hasExtra(EXTRA_SNOOZE_TIME)) {
      snoozeForTime(new DateTime(intent.getLongExtra(EXTRA_SNOOZE_TIME, 0L)));
    } else {
      FragmentManager fragmentManager = getSupportFragmentManager();
      SnoozeDialog fragmentByTag =
          (SnoozeDialog) fragmentManager.findFragmentByTag(FRAG_TAG_SNOOZE_DIALOG);
      if (fragmentByTag == null) {
        fragmentByTag = new SnoozeDialog();
        fragmentByTag.show(fragmentManager, FRAG_TAG_SNOOZE_DIALOG);
      }
      fragmentByTag.setOnCancelListener(this);
      fragmentByTag.setSnoozeCallback(this);
    }
  }

  @Override
  public void snoozeForTime(DateTime time) {
    taskDao.snooze(taskIds, time.getMillis());
    reminderService.scheduleAllAlarms(taskIds);
    notificationManager.cancel(taskIds);
    setResult(RESULT_OK);
    finish();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(EXTRA_PICKING_DATE_TIME, pickingDateTime);
  }

  @Override
  public void pickDateTime() {
    pickingDateTime = true;

    Intent intent = new Intent(this, DateAndTimePickerActivity.class);
    intent.putExtra(
        DateAndTimePickerActivity.EXTRA_TIMESTAMP, new DateTime().plusMinutes(30).getMillis());
    startActivityForResult(intent, REQUEST_DATE_TIME);
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    finish();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_DATE_TIME) {
      if (resultCode == RESULT_OK && data != null) {
        long timestamp = data.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, 0L);
        snoozeForTime(new DateTime(timestamp));
      } else {
        finish();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
