package org.tasks.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import java.util.ArrayList;
import java.util.List;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.data.SubtaskInfo;
import org.tasks.data.TaskContainer;
import org.tasks.data.TaskListQuery;
import org.tasks.locale.Locale;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxProvider;
import java.time.format.FormatStyle;
import timber.log.Timber;

class ScrollableViewsFactory implements RemoteViewsService.RemoteViewsFactory {

  private final int widgetId;
  private final TaskDao taskDao;
  private final DefaultFilterProvider defaultFilterProvider;
  private final CheckBoxProvider checkBoxProvider;
  private final Locale locale;
  private final SubtasksHelper subtasksHelper;
  private final Preferences preferences;
  private final Context context;
  private final int indentPadding;

  private boolean isDark;
  private boolean showDueDates;
  private boolean endDueDate;
  private boolean showCheckboxes;
  private float textSize;
  private float dueDateTextSize;
  private Filter filter;
  private int textColorPrimary;
  private int textColorSecondary;
  private boolean showFullTaskTitle;
  private boolean showDescription;
  private boolean showFullDescription;
  private int vPad;
  private int hPad;
  private boolean handleDueDateClick;
  private boolean showDividers;
  private boolean showSubtasks;
  private boolean isRtl;

  private List<TaskContainer> tasks = new ArrayList<>();

  ScrollableViewsFactory(
      SubtasksHelper subtasksHelper,
      Preferences preferences,
      Context context,
      int widgetId,
      TaskDao taskDao,
      DefaultFilterProvider defaultFilterProvider,
      CheckBoxProvider checkBoxProvider,
      Locale locale) {
    this.subtasksHelper = subtasksHelper;
    this.preferences = preferences;
    this.context = context;
    this.widgetId = widgetId;
    this.taskDao = taskDao;
    this.defaultFilterProvider = defaultFilterProvider;
    this.checkBoxProvider = checkBoxProvider;
    this.locale = locale;
    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    indentPadding = (int)(20 * metrics.density);
    updateSettings();
  }

  @Override
  public void onCreate() {}

  @Override
  public void onDataSetChanged() {
    updateSettings();
    tasks = taskDao.fetchTasks(subtasks -> getQuery(filter, subtasks));
  }

  @Override
  public void onDestroy() {
  }

  @Override
  public int getCount() {
    return tasks.size();
  }

  @Override
  public RemoteViews getViewAt(int position) {
    return buildUpdate(position);
  }

  @Override
  public RemoteViews getLoadingView() {
    return newRemoteView();
  }

  @Override
  public int getViewTypeCount() {
    return 1;
  }

  @Override
  public long getItemId(int position) {
    TaskContainer task = getTask(position);
    return task == null ? 0 : task.getId();
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }

  private Bitmap getCheckbox(Task task) {
    return checkBoxProvider.getWidgetCheckBox(task);
  }

  private RemoteViews newRemoteView() {
    return new RemoteViews(
        BuildConfig.APPLICATION_ID, isDark ? R.layout.widget_row_dark : R.layout.widget_row_light);
  }

  private RemoteViews buildUpdate(int position) {
    try {
      TaskContainer taskContainer = getTask(position);
      if (taskContainer == null) {
        return null;
      }
      Task task = taskContainer.getTask();
      int textColorTitle = textColorPrimary;

      RemoteViews row = newRemoteView();

      if (task.isHidden()) {
        textColorTitle = textColorSecondary;
        row.setViewVisibility(R.id.hidden_icon, View.VISIBLE);
      } else {
        row.setViewVisibility(R.id.hidden_icon, View.GONE);
      }

      if (task.isCompleted()) {
        textColorTitle = textColorSecondary;
        row.setInt(
            R.id.widget_text, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
      } else {
        row.setInt(R.id.widget_text, "setPaintFlags", Paint.ANTI_ALIAS_FLAG);
      }
      row.setFloat(R.id.widget_text, "setTextSize", textSize);
      if (showDueDates) {
        formatDueDate(row, task);
      } else {
        row.setViewVisibility(R.id.widget_due_bottom, View.GONE);
        row.setViewVisibility(R.id.widget_due_end, View.GONE);
        if (task.hasDueDate() && task.isOverdue()) {
          textColorTitle = context.getColor(R.color.overdue);
        }
      }
      if (showFullTaskTitle) {
        row.setInt(R.id.widget_text, "setMaxLines", Integer.MAX_VALUE);
      }
      row.setTextViewText(R.id.widget_text, task.getTitle());
      row.setTextColor(R.id.widget_text, textColorTitle);

      if (showDescription && task.hasNotes()) {
        row.setFloat(R.id.widget_description, "setTextSize", textSize);
        row.setTextViewText(R.id.widget_description, task.getNotes());
        row.setViewVisibility(R.id.widget_description, View.VISIBLE);
        if (showFullDescription) {
          row.setInt(R.id.widget_description, "setMaxLines", Integer.MAX_VALUE);
        }
      } else {
        row.setViewVisibility(R.id.widget_description, View.GONE);
      }

      row.setOnClickFillInIntent(
          R.id.widget_row,
          new Intent(WidgetClickActivity.EDIT_TASK)
              .putExtra(WidgetClickActivity.EXTRA_FILTER, filter)
              .putExtra(WidgetClickActivity.EXTRA_TASK, task));

      if (showCheckboxes) {
        row.setViewPadding(R.id.widget_complete_box, hPad, vPad, hPad, vPad);
        row.setImageViewBitmap(R.id.widget_complete_box, getCheckbox(task));
        row.setOnClickFillInIntent(
            R.id.widget_complete_box,
            new Intent(WidgetClickActivity.COMPLETE_TASK)
                .putExtra(WidgetClickActivity.EXTRA_TASK, task));
      } else {
        row.setViewPadding(R.id.widget_complete_box, hPad, 0, 0, 0);
        row.setInt(R.id.widget_complete_box, "setBackgroundResource", 0);
      }
      row.setViewPadding(R.id.top_padding, 0, vPad, 0, 0);
      row.setViewPadding(R.id.bottom_padding, 0, vPad, 0, 0);

      if (!showDividers) {
        row.setViewVisibility(R.id.divider, View.GONE);
      }

      if (showSubtasks && taskContainer.hasChildren()) {
        row.setOnClickFillInIntent(
            R.id.subtask_button,
            new Intent(WidgetClickActivity.TOGGLE_SUBTASKS)
                .putExtra(WidgetClickActivity.EXTRA_TASK, task)
                .putExtra(WidgetClickActivity.EXTRA_COLLAPSED, !taskContainer.isCollapsed()));
        row.setTextViewText(
            R.id.subtask_text,
            context
                .getResources()
                .getQuantityString(
                    R.plurals.subtask_count, taskContainer.children, taskContainer.children));
        row.setImageViewResource(
            R.id.subtask_icon,
            taskContainer.isCollapsed()
                ? R.drawable.ic_keyboard_arrow_up_black_18dp
                : R.drawable.ic_keyboard_arrow_down_black_18dp);
        row.setViewVisibility(R.id.subtask_button, View.VISIBLE);
      } else {
        row.setViewVisibility(R.id.subtask_button, View.GONE);
      }
      row.setInt(R.id.widget_row, "setLayoutDirection", locale.getDirectionality());

      int startPad = taskContainer.getIndent() * indentPadding;
      row.setViewPadding(R.id.widget_row, isRtl ? 0 : startPad, 0, isRtl ? startPad : 0, 0);

      return row;
    } catch (Exception e) {
      Timber.e(e);
    }

    return null;
  }

  private TaskContainer getTask(int position) {
    return position < tasks.size() ? tasks.get(position) : null;
  }

  private List<String> getQuery(Filter filter, SubtaskInfo subtasks) {
    List<String> queries = TaskListQuery.getQuery(preferences, filter, subtasks);
    int last = queries.size() - 1;
    queries.set(last, subtasksHelper.applySubtasksToWidgetFilter(filter, queries.get(last)));
    return queries;
  }

  private void formatDueDate(RemoteViews row, Task task) {
    int dueDateRes = endDueDate ? R.id.widget_due_end : R.id.widget_due_bottom;
    row.setViewVisibility(endDueDate ? R.id.widget_due_bottom : R.id.widget_due_end, View.GONE);
    boolean hasDueDate = task.hasDueDate();
    int endPad = hasDueDate && endDueDate ? 0 : hPad;
    row.setViewPadding(R.id.widget_text, isRtl ? endPad : 0, 0, isRtl ? 0 : endPad, 0);
    if (hasDueDate) {
      if (endDueDate) {
        row.setViewPadding(R.id.widget_due_end, hPad, vPad, hPad, vPad);
      }
      row.setViewVisibility(dueDateRes, View.VISIBLE);
      row.setTextViewText(
          dueDateRes,
          DateUtilities.getRelativeDateTime(
              context, task.getDueDate(), locale.getLocale(), FormatStyle.MEDIUM));
      row.setTextColor(
          dueDateRes,
          task.isOverdue() ? context.getColor(R.color.overdue) : textColorSecondary);
      row.setFloat(dueDateRes, "setTextSize", dueDateTextSize);
      if (handleDueDateClick) {
        row.setOnClickFillInIntent(
            dueDateRes,
            new Intent(WidgetClickActivity.RESCHEDULE_TASK)
                .putExtra(WidgetClickActivity.EXTRA_TASK, task));
      } else {
        row.setInt(dueDateRes, "setBackgroundResource", 0);
      }
    } else {
      row.setViewVisibility(dueDateRes, View.GONE);
    }
  }

  private void updateSettings() {
    WidgetPreferences widgetPreferences = new WidgetPreferences(context, preferences, widgetId);
    vPad = widgetPreferences.getWidgetSpacing();
    hPad = (int) context.getResources().getDimension(R.dimen.widget_padding);
    handleDueDateClick = widgetPreferences.rescheduleOnDueDateClick();
    showFullTaskTitle = widgetPreferences.showFullTaskTitle();
    showDescription = widgetPreferences.showDescription();
    showFullDescription = widgetPreferences.showFullDescription();
    isDark = widgetPreferences.getThemeIndex() > 0;
    textColorPrimary =
        context.getColor(isDark ? R.color.white_87 : R.color.black_87);
    textColorSecondary =
        context.getColor(isDark ? R.color.white_60 : R.color.black_60);
    int dueDatePosition = widgetPreferences.getDueDatePosition();
    showDueDates = dueDatePosition != 2;
    endDueDate = dueDatePosition != 1;
    showCheckboxes = widgetPreferences.showCheckboxes();
    textSize = widgetPreferences.getFontSize();
    dueDateTextSize = Math.max(10, textSize - 2);
    filter = defaultFilterProvider.getFilterFromPreference(widgetPreferences.getFilterId());
    showDividers = widgetPreferences.showDividers();
    showSubtasks = widgetPreferences.showSubtasks();
    isRtl = locale.getDirectionality() == View.LAYOUT_DIRECTION_RTL;
  }
}
