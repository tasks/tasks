package org.tasks.widget;

import android.content.Context;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.service.Upgrader;

import org.tasks.R;
import org.tasks.Strings;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.QueryPreferences;

import java.util.HashSet;

import timber.log.Timber;

public class WidgetPreferences implements QueryPreferences {

  private final Context context;
  private final Preferences preferences;
  private final int widgetId;

  public WidgetPreferences(Context context, Preferences preferences, int widgetId) {
    this.context = context;
    this.preferences = preferences;
    this.widgetId = widgetId;
  }

  boolean showHeader() {
    return getBoolean(R.string.p_widget_show_header, true);
  }

  boolean showTitle() {
    return getBoolean(R.string.p_widget_show_title, true);
  }

  boolean showCheckboxes() {
    return getBoolean(R.string.p_widget_show_checkboxes, true);
  }

  boolean showSettings() {
    return getBoolean(R.string.p_widget_show_settings, true);
  }

  boolean showMenu() {
    return getBoolean(R.string.p_widget_show_menu, true);
  }

  boolean showFullTaskTitle() {
    return getBoolean(R.string.p_widget_show_full_task_title, false);
  }

  boolean showDescription() {
    return getBoolean(R.string.p_widget_show_description, true);
  }

  boolean showFullDescription() {
    return getBoolean(R.string.p_widget_show_full_description, false);
  }

  boolean showDividers() {
    return getBoolean(R.string.p_widget_show_dividers, true);
  }

  boolean showSubtasks() {
    return getBoolean(R.string.p_widget_show_subtasks, true);
  }

  boolean showStartDates() {
    return getBoolean(R.string.p_widget_show_start_dates, true);
  }

  boolean showPlaces() {
    return getBoolean(R.string.p_widget_show_places, true);
  }

  boolean showLists() {
    return getBoolean(R.string.p_widget_show_lists, true);
  }

  boolean showTags() {
    return getBoolean(R.string.p_widget_show_tags, true);
  }

  public int getDueDatePosition() {
    return getIntegerFromString(R.string.p_widget_due_date_position, 0);
  }

  public void setCollapsed(Iterable<Long> collapsed) {
    setString(R.string.p_widget_collapsed, Joiner.on(",").join(collapsed));
  }

  public HashSet<Long> getCollapsed() {
    String value = getString(R.string.p_widget_collapsed);
    HashSet<Long> collapsed = new HashSet<>();
    if (!Strings.isNullOrEmpty(value)) {
      for (String entry : Splitter.on(",").split(value)) {
        try {
          collapsed.add(Long.parseLong(entry));
        } catch (NumberFormatException e) {
          Timber.e(e);
        }
      }
    }
    return collapsed;
  }

  int getWidgetSpacing() {
    return getSpacing(R.string.p_widget_spacing);
  }

  int getHeaderSpacing() {
    return getSpacing(R.string.p_widget_header_spacing);
  }

  int getHeaderLayout() {
    switch (getIntegerFromString(R.string.p_widget_header_spacing, 0)) {
      case 1:
        return R.layout.widget_title_compact;
      case 2:
        return R.layout.widget_title_none;
      default:
        return R.layout.widget_title_default;
    }
  }

  private int getSpacing(int pref) {
    int spacing = getIntegerFromString(pref, 0);
    if (spacing == 2) {
      return 0;
    }
    int dimen = spacing == 1 ? R.dimen.widget_padding_compact : R.dimen.widget_padding;
    return (int) context.getResources().getDimension(dimen);
  }

  int getFontSize() {
    return getInt(R.string.p_widget_font_size, 16);
  }

  public String getFilterId() {
    return getString(R.string.p_widget_filter);
  }

  public int getThemeIndex() {
    return getInt(R.string.p_widget_theme, 3);
  }

  public int getColor() {
    int color = getInt(R.string.p_widget_color_v2, 0);
    if (color != 0) {
      return color;
    }
    int index = getInt(R.string.p_widget_color, -1);
    color = context.getColor(Upgrader.getLegacyColor(index, R.color.blue_500));
    setInt(R.string.p_widget_color_v2, color);
    return color;
  }

  public void setColor(int color) {
    setInt(R.string.p_widget_color_v2, color);
  }

  int getHeaderOpacity() {
    return getAlphaValue(R.string.p_widget_header_opacity);
  }

  int getFooterOpacity() {
    return getAlphaValue(R.string.p_widget_footer_opacity);
  }

  int getRowOpacity() {
    return getAlphaValue(R.string.p_widget_opacity);
  }

  boolean openOnFooterClick() {
    return getIntegerFromString(R.string.p_widget_footer_click, 0) == 1;
  }

  boolean rescheduleOnDueDateClick() {
    return getIntegerFromString(R.string.p_widget_due_date_click, 0) == 0;
  }

  private int getAlphaValue(int resId) {
    return (int) (getInt(resId, 100) / 100.0 * 255.0);
  }

  public void setTheme(int index) {
    setInt(R.string.p_widget_theme, index);
  }

  public String getKey(int resId) {
    return context.getString(resId) + widgetId;
  }

  public void setFilter(String filterPreferenceValue) {
    setCollapsed(new HashSet<>());
    preferences.setString(getKey(R.string.p_widget_filter), filterPreferenceValue);
  }

  public boolean getCompact() {
    return getBoolean(R.string.p_widget_compact, false);
  }

  public void setCompact(boolean value) {
    setBoolean(R.string.p_widget_compact, value);
  }

  private int getInt(int resId, int defValue) {
    return preferences.getInt(getKey(resId), defValue);
  }

  private int getIntegerFromString(int resId, int defValue) {
    return preferences.getIntegerFromString(getKey(resId), defValue);
  }

  private boolean getBoolean(int resId, boolean defValue) {
    return preferences.getBoolean(getKey(resId), defValue);
  }

  private String getString(int resId) {
    return preferences.getStringValue(getKey(resId));
  }

  private void setInt(int resId, int value) {
    preferences.setInt(getKey(resId), value);
  }

  private void setBoolean(int resId, boolean value) {
    preferences.setBoolean(getKey(resId), value);
  }

  private void setString(int resId, String value) {
    preferences.setString(getKey(resId), value);
  }

  public void maintainExistingConfiguration() {
    int rowOpacity = getInt(R.string.p_widget_opacity, 100);
    setInt(R.string.p_widget_header_opacity, rowOpacity);
    setInt(R.string.p_widget_footer_opacity, rowOpacity);
    boolean showDueDate = getBoolean(R.string.p_widget_show_due_date, true);
    setString(R.string.p_widget_due_date_position, showDueDate ? "1" : "2"); // below or hidden
    setBoolean(R.string.p_widget_show_dividers, false); // no dividers
    setBoolean(R.string.p_widget_show_menu, false); // no menu
    setString(R.string.p_widget_spacing, "1"); // compact
    setBoolean(R.string.p_widget_show_description, false); // no description
  }

  @Override
  public int getSortMode() {
    return getInt(R.string.p_widget_sort, SortHelper.SORT_AUTO);
  }

  @Override
  public int getGroupMode() {
    return getInt(R.string.p_widget_group, SortHelper.GROUP_NONE);
  }

  @Override
  public boolean isManualSort() {
    return getBoolean(R.string.p_widget_sort_manual, false);
  }

  @Override
  public boolean isAstridSort() {
    return getBoolean(R.string.p_widget_sort_astrid, false);
  }

  @Override
  public boolean getSortAscending() {
    return getBoolean(R.string.p_widget_sort_ascending, true);
  }

  @Override
  public boolean getGroupAscending() {
    return getBoolean(R.string.p_widget_group_ascending, false);
  }

  @Override
  public boolean getShowHidden() {
    return getBoolean(R.string.p_widget_show_hidden, true);
  }

  @Override
  public boolean getShowCompleted() {
    return getBoolean(R.string.p_widget_show_completed, false);
  }

  @Override
  public boolean getAlwaysDisplayFullDate() { return preferences.getAlwaysDisplayFullDate(); }

  @Override
  public boolean getCompletedTasksAtBottom() {
    return preferences.getCompletedTasksAtBottom();
  }

  @Override
  public void setCompletedTasksAtBottom(boolean value) {
    preferences.setBoolean(R.string.p_completed_tasks_at_bottom, value);
  }

  @Override
  public void setSortMode(int sortMode) {
    setInt(R.string.p_widget_sort, sortMode);
  }

  @Override
  public void setGroupMode(int groupMode) {
    setInt(R.string.p_widget_group, groupMode);
  }

  @Override
  public void setManualSort(boolean isManualSort) {
    setBoolean(R.string.p_widget_sort_manual, isManualSort);
  }

  @Override
  public void setAstridSort(boolean isAstridSort) {
    setBoolean(R.string.p_widget_sort_astrid, isAstridSort);
  }

  @Override
  public void setSortAscending(boolean ascending) {
    setBoolean(R.string.p_widget_sort_ascending, ascending);
  }

  @Override
  public void setGroupAscending(boolean ascending) {
    setBoolean(R.string.p_widget_group_ascending, ascending);
  }

  @Override
  public void setAlwaysDisplayFullDate(boolean noWeekday) {
    preferences.setAlwaysDisplayFullDate(noWeekday);
  }

  @Override
  public int getCompletedMode() {
    return preferences.getCompletedMode();
  }

  @Override
  public void setCompletedMode(int mode) {
    preferences.setCompletedMode(mode);
  }

  @Override
  public boolean getCompletedAscending() {
    return preferences.getCompletedAscending();
  }

  @Override
  public void setCompletedAscending(boolean ascending) {
    preferences.setCompletedAscending(ascending);
  }

  @Override
  public int getSubtaskMode() {
    return preferences.getSubtaskMode();
  }

  @Override
  public void setSubtaskMode(int mode) {
    preferences.setSubtaskMode(mode);
  }

  @Override
  public boolean getSubtaskAscending() {
    return preferences.getSubtaskAscending();
  }

  @Override
  public void setSubtaskAscending(boolean ascending) {
    preferences.setSubtaskAscending(ascending);
  }
}
