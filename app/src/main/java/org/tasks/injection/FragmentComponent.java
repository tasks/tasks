package org.tasks.injection;

import androidx.annotation.NonNull;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerControlSet;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.HideUntilControlSet;
import com.todoroo.astrid.ui.ReminderControlSet;
import dagger.Subcomponent;
import org.tasks.fragments.CommentBarFragment;
import org.tasks.preferences.fragments.Advanced;
import org.tasks.preferences.fragments.Backups;
import org.tasks.preferences.fragments.DashClock;
import org.tasks.preferences.fragments.Debug;
import org.tasks.preferences.fragments.HelpAndFeedback;
import org.tasks.preferences.fragments.LookAndFeel;
import org.tasks.preferences.fragments.MainSettingsFragment;
import org.tasks.preferences.fragments.Notifications;
import org.tasks.preferences.fragments.ScrollableWidget;
import org.tasks.preferences.fragments.Synchronization;
import org.tasks.preferences.fragments.TaskDefaults;
import org.tasks.preferences.fragments.TaskerListNotification;
import org.tasks.ui.CalendarControlSet;
import org.tasks.ui.DeadlineControlSet;
import org.tasks.ui.DescriptionControlSet;
import org.tasks.ui.LocationControlSet;
import org.tasks.ui.NavigationDrawerFragment;
import org.tasks.ui.PriorityControlSet;
import org.tasks.ui.RemoteListFragment;
import org.tasks.ui.SubtaskControlSet;
import org.tasks.ui.TaskListViewModel;

@Subcomponent(modules = FragmentModule.class)
public interface FragmentComponent {

  void inject(TimerControlSet timerControlSet);

  void inject(TaskEditFragment taskEditFragment);

  void inject(NavigationDrawerFragment navigationDrawerFragment);

  void inject(PriorityControlSet priorityControlSet);

  void inject(RepeatControlSet repeatControlSet);

  void inject(CommentBarFragment commentBarFragment);

  void inject(EditTitleControlSet editTitleControlSet);

  void inject(FilesControlSet filesControlSet);

  void inject(TagsControlSet tagsControlSet);

  void inject(HideUntilControlSet hideUntilControlSet);

  void inject(ReminderControlSet reminderControlSet);

  void inject(DeadlineControlSet deadlineControlSet);

  void inject(DescriptionControlSet descriptionControlSet);

  void inject(CalendarControlSet calendarControlSet);

  void inject(TaskListFragment taskListFragment);

  void inject(RemoteListFragment remoteListFragment);

  void inject(LocationControlSet locationControlSet);

  void inject(SubtaskControlSet subtaskControlSet);

  void inject(TaskListViewModel taskListViewModel);

  void inject(@NonNull HelpAndFeedback helpAndFeedback);

  void inject(@NonNull LookAndFeel lookAndFeel);

  void inject(@NonNull Synchronization synchronization);

  void inject(@NonNull Debug debug);

  void inject(@NonNull MainSettingsFragment mainSettingsFragment);

  void inject(@NonNull Backups backups);

  void inject(@NonNull Advanced advanced);

  void inject(@NonNull Notifications notifications);

  void inject(@NonNull TaskDefaults taskDefaults);

  void inject(@NonNull ScrollableWidget scrollableWidget);

  void inject(@NonNull DashClock dashClock);

  void inject(@NonNull TaskerListNotification taskerListNotification);
}
