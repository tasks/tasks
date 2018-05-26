package org.tasks.widget;

import android.content.Context;
import org.tasks.R;
import org.tasks.preferences.Preferences;

class WidgetPreferences {

  private final Context context;
  private final Preferences preferences;
  private final int widgetId;

  WidgetPreferences(Context context, Preferences preferences, int widgetId) {
    this.context = context;
    this.preferences = preferences;
    this.widgetId = widgetId;
  }

  boolean showDueDate() {
    return preferences.getBoolean(getKey(R.string.p_widget_show_due_date), true);
  }

  boolean showHeader() {
    return preferences.getBoolean(getKey(R.string.p_widget_show_header), true);
  }

  boolean showCheckboxes() {
    return preferences.getBoolean(getKey(R.string.p_widget_show_checkboxes), true);
  }

  boolean showSettings() {
    return preferences.getBoolean(getKey(R.string.p_widget_show_settings), true);
  }

  int getFontSize() {
    return preferences.getInt(getKey(R.string.p_widget_font_size), 16);
  }

  void setFontSize(int value) {
    preferences.setInt(getKey(R.string.p_widget_font_size), value);
  }

  String getFilterId() {
    return preferences.getStringValue(getKey(R.string.p_widget_filter));
  }

  int getThemeIndex() {
    return preferences.getInt(getKey(R.string.p_widget_theme), 0);
  }

  int getColorIndex() {
    return preferences.getInt(getKey(R.string.p_widget_color), 0);
  }

  public int getOpacity() {
    return preferences.getInt(getKey(R.string.p_widget_opacity), 100);
  }

  public void setOpacity(int value) {
    preferences.setInt(getKey(R.string.p_widget_opacity), value);
  }

  public void setColor(int index) {
    preferences.setInt(getKey(R.string.p_widget_color), index);
  }

  public void setTheme(int index) {
    preferences.setInt(getKey(R.string.p_widget_theme), index);
  }

  private String getKey(int resId) {
    return context.getString(resId) + widgetId;
  }

  public void setFilter(String filterPreferenceValue) {
    preferences.setString(getKey(R.string.p_widget_filter), filterPreferenceValue);
  }
}
