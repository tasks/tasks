package org.tasks.widget;

import static androidx.core.content.ContextCompat.getColor;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import java.util.ArrayList;
import java.util.List;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.data.TaskContainer;
import org.tasks.data.TaskListQuery;
import org.tasks.locale.Locale;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxProvider;
import org.threeten.bp.format.FormatStyle;
import timber.log.Timber;

class ScrollableViewsFactory implements RemoteViewsService.RemoteViewsFactory {

  private final int widgetId;
  private final TaskDao taskDao;
  private final DefaultFilterProvider defaultFilterProvider;
  private final CheckBoxProvider checkBoxProvider;
  private final Locale locale;
  private final SubtasksHelper subtasksHelper;
  private final Preferences preferences;
  private final WidgetPreferences widgetPreferences;
  private final Context context;
  private final int indentPadding;

  private boolean showDueDates;
  private boolean bottomDueDate;
  private boolean showCheckboxes;
  private float textSize;
  private float dueDateTextSize;
  private Filter filter;
  private int textColorPrimary;
  private int textColorSecondary;

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
    widgetPreferences = new WidgetPreferences(context, preferences, widgetId);
    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    indentPadding = (int)(20 * metrics.density);
    updateSettings();
  }

  @Override
  public void onCreate() {}

  @Override
  public void onDataSetChanged() {
    updateSettings();
    tasks =
        taskDao.fetchTasks(
            (includeGoogleSubtasks, includeCaldavSubtasks) ->
                getQuery(filter, includeGoogleSubtasks, includeCaldavSubtasks));
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
    return new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_row);
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

  private RemoteViews buildUpdate(int position) {
    try {
      TaskContainer taskContainer = getTask(position);
      if (taskContainer == null) {
        return null;
      }
      Task task = taskContainer.getTask();
      String textContent;
      int textColorTitle = textColorPrimary;

      textContent = task.getTitle();

      RemoteViews row = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_row);

      if (task.isHidden()) {
        textColorTitle = textColorSecondary;
        row.setViewVisibility(R.id.hidden_icon, View.VISIBLE);
        row.setInt(R.id.hidden_icon, "setColorFilter", textColorSecondary);
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
      row.setFloat(R.id.widget_description, "setTextSize", textSize);
      if (showDueDates) {
        formatDueDate(row, task);
      } else {
        row.setViewVisibility(R.id.widget_due_bottom, View.GONE);
        row.setViewVisibility(R.id.widget_due_end, View.GONE);
        if (task.hasDueDate() && task.isOverdue()) {
          textColorTitle = getColor(context, R.color.overdue);
        }
      }

      row.setInt(
          R.id.widget_text,
          "setMaxLines",
          widgetPreferences.showFullTaskTitle() ? Integer.MAX_VALUE : 1);

      boolean showDescription = task.hasNotes() && widgetPreferences.showDescription();

      if (showDescription) {
        row.setTextViewText(R.id.widget_description, task.getNotes());
        row.setViewVisibility(R.id.widget_description, View.VISIBLE);
        row.setTextColor(R.id.widget_description, textColorSecondary);
        row.setInt(
            R.id.widget_description,
            "setMaxLines",
            widgetPreferences.showFullDescription() ? Integer.MAX_VALUE : 2);
      } else {
        row.setViewVisibility(R.id.widget_description, View.GONE);
      }

      row.setTextViewText(R.id.widget_text, textContent);
      row.setTextColor(R.id.widget_text, textColorTitle);
      row.setImageViewBitmap(R.id.widget_complete_box, getCheckbox(task));

      Intent editIntent = new Intent(WidgetClickActivity.EDIT_TASK);
      editIntent.putExtra(WidgetClickActivity.EXTRA_FILTER, filter);
      editIntent.putExtra(WidgetClickActivity.EXTRA_TASK, task);
      row.setOnClickFillInIntent(R.id.widget_row, editIntent);

      int theme = widgetPreferences.getThemeIndex();
      if (atLeastLollipop()) {
        row.setInt(
            R.id.widget_row,
            "setBackgroundResource",
            theme == 0 ? R.drawable.widget_ripple_light : R.drawable.widget_ripple_dark);
      }

      int hPad = (int) context.getResources().getDimension(R.dimen.widget_padding);
      int vPad = widgetPreferences.getWidgetSpacing();
      row.setViewPadding(R.id.widget_complete_box, hPad, vPad, hPad, vPad);
      if (showCheckboxes) {
        row.setViewVisibility(R.id.widget_complete_box, View.VISIBLE);
        Intent completeIntent = new Intent(WidgetClickActivity.COMPLETE_TASK);
        completeIntent.putExtra(WidgetClickActivity.EXTRA_TASK, task);
        row.setOnClickFillInIntent(R.id.widget_complete_box, completeIntent);
        row.setViewPadding(R.id.start_padding, 0, 0, 0, 0);
      } else {
        row.setViewVisibility(R.id.widget_complete_box, View.GONE);
        row.setViewPadding(R.id.start_padding, hPad, 0, 0, 0);
      }
      row.setViewPadding(R.id.top_padding, 0, vPad, 0, 0);
      row.setViewPadding(R.id.bottom_padding, 0, vPad, 0, 0);

      if (widgetPreferences.showDividers()) {
        int dividerColor =
            ContextCompat.getColor(context, theme == 0 ? R.color.black_12 : R.color.white_12);
        row.setViewVisibility(R.id.divider, View.VISIBLE);
        row.setImageViewBitmap(R.id.divider, getSolidBackground(dividerColor));
      } else {
        row.setViewVisibility(R.id.divider, View.GONE);
      }

      if (taskContainer.hasChildren()) {
        Intent toggleSubtasks = new Intent(WidgetClickActivity.TOGGLE_SUBTASKS);
        toggleSubtasks.putExtra(WidgetClickActivity.EXTRA_TASK, task);
        toggleSubtasks.putExtra(WidgetClickActivity.EXTRA_COLLAPSED, !taskContainer.isCollapsed());
        row.setOnClickFillInIntent(R.id.subtask_button, toggleSubtasks);
        row.setInt(
            R.id.subtask_button,
            "setBackgroundResource",
            theme == 0 ? R.drawable.widget_chip_light : R.drawable.widget_chip_dark);
        row.setTextColor(R.id.subtask_text, textColorSecondary);
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
        row.setInt(R.id.subtask_icon, "setColorFilter", textColorSecondary);
        row.setViewVisibility(R.id.subtask_button, View.VISIBLE);
      } else {
        row.setViewVisibility(R.id.subtask_button, View.GONE);
      }
      row.setInt(
          R.id.widget_row, "setLayoutDirection", Locale.getInstance(context).getDirectionality());

      row.setViewPadding(
          R.id.widget_row,
          taskContainer.getIndent() * indentPadding,
          0,
          0,
          0);

      return row;
    } catch (Exception e) {
      Timber.e(e);
    }

    return null;
  }

  private static Bitmap getSolidBackground(@ColorInt int bgColor) {
    Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888); // Create a Bitmap
    new Canvas(bitmap).drawColor(bgColor); // Set the color
    return bitmap;
  }

  private TaskContainer getTask(int position) {
    return position < tasks.size() ? tasks.get(position) : null;
  }

  private List<String> getQuery(
      Filter filter, boolean includeGoogleSubtasks, boolean includeCaldavSubtasks) {
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.scrollable_widget);
    rv.setTextViewText(R.id.widget_title, filter.listingTitle);
    rv.setInt(R.id.widget, "setLayoutDirection", Locale.getInstance(context).getDirectionality());
    appWidgetManager.partiallyUpdateAppWidget(widgetId, rv);
    List<String> queries =
        TaskListQuery.getQuery(
            preferences, filter, includeGoogleSubtasks, includeCaldavSubtasks);
    int last = queries.size() - 1;
    queries.set(last, subtasksHelper.applySubtasksToWidgetFilter(filter, queries.get(last)));
    return queries;
  }

  private void formatDueDate(RemoteViews row, Task task) {
    int dueDateRes = bottomDueDate ? R.id.widget_due_bottom : R.id.widget_due_end;
    row.setViewVisibility(bottomDueDate ? R.id.widget_due_end : R.id.widget_due_bottom, View.GONE);
    if (task.hasDueDate()) {
      row.setViewVisibility(dueDateRes, View.VISIBLE);
      row.setTextViewText(
          dueDateRes,
          DateUtilities.getRelativeDateTime(
              context, task.getDueDate(), locale.getLocale(), FormatStyle.MEDIUM));
      row.setTextColor(
          dueDateRes,
          task.isOverdue() ? getColor(context, R.color.overdue) : textColorSecondary);
      row.setFloat(dueDateRes, "setTextSize", dueDateTextSize);
    } else {
      row.setViewVisibility(dueDateRes, View.GONE);
    }
  }

  private void updateSettings() {
    boolean isDark = widgetPreferences.getThemeIndex() > 0;
    textColorPrimary =
        ContextCompat.getColor(context, isDark ? R.color.white_87 : R.color.black_87);
    textColorSecondary =
        ContextCompat.getColor(context, isDark ? R.color.white_60 : R.color.black_60);
    int dueDatePosition = widgetPreferences.getDueDatePosition();
    showDueDates = dueDatePosition != 2;
    bottomDueDate = dueDatePosition == 1;
    showCheckboxes = widgetPreferences.showCheckboxes();
    textSize = widgetPreferences.getFontSize();
    dueDateTextSize = Math.max(10, textSize - 2);
    filter = defaultFilterProvider.getFilterFromPreference(widgetPreferences.getFilterId());
  }
}
