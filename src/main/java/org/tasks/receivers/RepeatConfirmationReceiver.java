package org.tasks.receivers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.WindowManager;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Flags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

import static org.tasks.date.DateTimeUtils.newDate;

public class RepeatConfirmationReceiver extends InjectingBroadcastReceiver {

    private static final Logger log = LoggerFactory.getLogger(RepeatConfirmationReceiver.class);

    private final Property<?>[] REPEAT_RESCHEDULED_PROPERTIES =
            new Property<?>[]{
                    Task.ID,
                    Task.TITLE,
                    Task.DUE_DATE,
                    Task.HIDE_UNTIL,
                    Task.REPEAT_UNTIL
            };

    @Inject TaskService taskService;

    private final Activity activity;

    public RepeatConfirmationReceiver(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, 0);

        if (taskId > 0) {
            long oldDueDate = intent.getLongExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, 0);
            long newDueDate = intent.getLongExtra(AstridApiConstants.EXTRAS_NEW_DUE_DATE, 0);
            Task task = taskService.fetchById(taskId, REPEAT_RESCHEDULED_PROPERTIES);

            try {
                showSnackbar(activity.findViewById(R.id.task_list_body), task, oldDueDate, newDueDate);
            } catch (WindowManager.BadTokenException e) { // Activity not running when tried to show dialog--rebroadcast
                log.error(e.getMessage(), e);
                new Thread() {
                    @Override
                    public void run() {
                        context.sendBroadcast(intent);
                    }
                }.start();
            }
        }
    }

    private void showSnackbar(View view, final Task task, final long oldDueDate, final long newDueDate) {
        String dueDateString = getRelativeDateAndTimeString(activity, newDueDate);
        String snackbarText = activity.getString(R.string.repeat_snackbar, task.getTitle(), dueDateString);

        Snackbar.make(view, snackbarText, Snackbar.LENGTH_LONG)
                .setAction(R.string.DLG_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        task.setDueDate(oldDueDate);
                        task.setCompletionDate(0L);
                        long hideUntil = task.getHideUntil();
                        if (hideUntil > 0) {
                            task.setHideUntil(hideUntil - (newDueDate - oldDueDate));
                        }
                        taskService.save(task);
                        Flags.set(Flags.REFRESH);
                    }
                })
                .show();
    }

    private String getRelativeDateAndTimeString(Context context, long date) {
        String dueString = date > 0 ? DateUtilities.getRelativeDay(context, date, false) : "";
        if (Task.hasDueTime(date)) {
            // TODO: localize this
            dueString = String.format("%s at %s", dueString, //$NON-NLS-1$
                    DateUtilities.getTimeString(context, newDate(date)));
        }
        return dueString;
    }
}
