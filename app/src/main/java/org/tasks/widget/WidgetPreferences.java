package org.tasks.widget;

import android.content.Context;
import com.todoroo.astrid.service.Upgrader;
import org.tasks.R;
import org.tasks.preferences.Preferences;

public class WidgetPreferences {

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

  public int getDueDatePosition() {
    return getIntegerFromString(R.string.p_widget_due_date_position, 0);
  }

  int getWidgetSpacing() {
    int spacing = getIntegerFromString(R.string.p_widget_spacing, 0);
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
    return getInt(R.string.p_widget_theme, 0);
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
    preferences.setString(getKey(R.string.p_widget_filter), filterPreferenceValue);
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
}
