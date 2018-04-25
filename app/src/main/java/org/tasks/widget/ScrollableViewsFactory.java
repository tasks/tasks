package org.tasks.widget;

import static android.support.v4.content.ContextCompat.getColor;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.google.common.collect.ObjectArrays;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.data.GoogleTask;
import org.tasks.locale.Locale;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.WidgetTheme;
import org.tasks.ui.WidgetCheckBoxes;
import timber.log.Timber;

class ScrollableViewsFactory implements RemoteViewsService.RemoteViewsFactory {

  private final WidgetCheckBoxes checkBoxes;
  private final ThemeCache themeCache;
  private final int widgetId;
  private final TaskDao taskDao;
  private final DefaultFilterProvider defaultFilterProvider;
  private final SubtasksHelper subtasksHelper;
  private final Preferences preferences;
  private final WidgetPreferences widgetPreferences;
  private final Context context;

  private boolean showDueDates;
  private boolean showCheckboxes;
  private float textSize;
  private float dueDateTextSize;
  private String filterId;
  private int textColorPrimary;
  private int textColorSecondary;

  private Cursor cursor;

  ScrollableViewsFactory(
      SubtasksHelper subtasksHelper,
      Preferences preferences,
      Context context,
      int widgetId,
      TaskDao taskDao,
      DefaultFilterProvider defaultFilterProvider,
      WidgetCheckBoxes checkBoxes,
      ThemeCache themeCache) {
    this.subtasksHelper = subtasksHelper;
    this.preferences = preferences;
    this.context = context;
    this.widgetId = widgetId;
    this.taskDao = taskDao;
    this.defaultFilterProvider = defaultFilterProvider;
    this.checkBoxes = checkBoxes;
    this.themeCache = themeCache;

    widgetPreferences = new WidgetPreferences(context, preferences, widgetId);

    updateSettings();
  }

  @Override
  public void onCreate() {
    cursor = getCursor();
  }

  @Override
  public void onDataSetChanged() {
    if (cursor != null) {
      cursor.close();
    }
    cursor = getCursor();
  }

  @Override
  public void onDestroy() {
    if (cursor != null) {
      cursor.close();
    }
  }

  @Override
  public int getCount() {
    return cursor.getCount();
  }

  @Override
  public RemoteViews getViewAt(int position) {
    return buildUpdate(position);
  }

  @Override
  public RemoteViews getLoadingView() {
    return null;
  }

  @Override
  public int getViewTypeCount() {
    return 1;
  }

  @Override
  public long getItemId(int position) {
    Task task = getTask(position);
    return task == null ? 0 : task.getId();
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }

  private Bitmap getCheckbox(Task task) {
    if (task.isCompleted()) {
      return checkBoxes.getCompletedCheckbox(task.getPriority());
    } else if (TextUtils.isEmpty(task.getRecurrence())) {
      return checkBoxes.getCheckBox(task.getPriority());
    } else {
      return checkBoxes.getRepeatingCheckBox(task.getPriority());
    }
  }

  private RemoteViews buildUpdate(int position) {
    try {
      Task task = getTask(position);
      if (task == null) {
        return null;
      }
      String textContent;
      int textColorTitle = textColorPrimary;

      textContent = task.getTitle();

      RemoteViews row = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_row);

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
        row.setViewVisibility(R.id.widget_due_date, View.GONE);
        if (task.hasDueDate() && task.isOverdue()) {
          textColorTitle = getColor(context, R.color.overdue);
        }
      }

      row.setTextViewText(R.id.widget_text, textContent);
      row.setTextColor(R.id.widget_text, textColorTitle);
      row.setImageViewBitmap(R.id.widget_complete_box, getCheckbox(task));

      long taskId = task.getId();
      Intent editIntent = new Intent(TasksWidget.EDIT_TASK);
      editIntent.putExtra(TasksWidget.EXTRA_FILTER_ID, filterId);
      editIntent.putExtra(TasksWidget.EXTRA_ID, taskId);
      row.setOnClickFillInIntent(R.id.widget_row, editIntent);

      if (showCheckboxes) {
        row.setViewVisibility(R.id.widget_complete_box, View.VISIBLE);
        Intent completeIntent = new Intent(TasksWidget.COMPLETE_TASK);
        completeIntent.putExtra(TasksWidget.EXTRA_ID, taskId);
        row.setOnClickFillInIntent(R.id.widget_complete_box, completeIntent);
      } else {
        row.setViewVisibility(R.id.widget_complete_box, View.GONE);
      }

      if (atLeastJellybeanMR1()) {
        row.setInt(
            R.id.widget_row, "setLayoutDirection", Locale.getInstance(context).getDirectionality());
      }

      return row;
    } catch (Exception e) {
      Timber.e(e);
    }

    return null;
  }

  private Cursor getCursor() {
    updateSettings();
    Filter filter = defaultFilterProvider.getFilterFromPreference(filterId);
    return taskDao.getCursor(getQuery(filter), getProperties(filter));
  }

  private Task getTask(int position) {
    return cursor.moveToPosition(position) ? new Task(cursor) : null;
  }

  private String getQuery(Filter filter) {
    int sort = preferences.getSortMode();
    if (sort == 0) {
      sort = SortHelper.SORT_WIDGET;
    }
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.scrollable_widget);
    rv.setTextViewText(R.id.widget_title, filter.listingTitle);
    if (atLeastJellybeanMR1()) {
      rv.setInt(R.id.widget, "setLayoutDirection", Locale.getInstance(context).getDirectionality());
    }
    appWidgetManager.partiallyUpdateAppWidget(widgetId, rv);
    String query =
        SortHelper.adjustQueryForFlagsAndSort(preferences, filter.getSqlQuery(), sort)
            .replaceAll("LIMIT \\d+", "");
    return subtasksHelper.applySubtasksToWidgetFilter(filter, query);
  }

  private Property<?>[] getProperties(Filter filter) {
    return filter instanceof GtasksFilter
        ? ObjectArrays.concat(Task.PROPERTIES, new Property<?>[] {GoogleTask.ORDER}, Property.class)
        : Task.PROPERTIES;
  }

  private void formatDueDate(RemoteViews row, Task task) {
    if (task.hasDueDate()) {
      Resources resources = context.getResources();
      row.setViewVisibility(R.id.widget_due_date, View.VISIBLE);
      row.setTextViewText(
          R.id.widget_due_date,
          task.isCompleted()
              ? resources.getString(
                  R.string.TAd_completed,
                  DateUtilities.getRelativeDateStringWithTime(context, task.getCompletionDate()))
              : DateUtilities.getRelativeDateStringWithTime(context, task.getDueDate()));
      //noinspection ResourceAsColor
      row.setTextColor(
          R.id.widget_due_date,
          task.isOverdue() ? getColor(context, R.color.overdue) : textColorSecondary);
      row.setFloat(R.id.widget_due_date, "setTextSize", dueDateTextSize);
    } else {
      row.setViewVisibility(R.id.widget_due_date, View.GONE);
    }
  }

  private void updateSettings() {
    WidgetTheme widgetTheme = themeCache.getWidgetTheme(widgetPreferences.getThemeIndex());
    textColorPrimary = widgetTheme.getTextColorPrimary();
    textColorSecondary = widgetTheme.getTextColorSecondary();
    showDueDates = widgetPreferences.showDueDate();
    showCheckboxes = widgetPreferences.showCheckboxes();
    textSize = widgetPreferences.getFontSize();
    dueDateTextSize = Math.max(10, textSize - 2);
    filterId = widgetPreferences.getFilterId();
  }
}
