package org.tasks.widget;

import android.content.Context;
import androidx.core.content.ContextCompat;
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

  boolean showMenu() {
    return preferences.getBoolean(getKey(R.string.p_widget_show_menu), true);
  }

  boolean showFullTaskTitle() {
    return preferences.getBoolean(getKey(R.string.p_widget_show_full_task_title), false);
  }

  boolean showDescription() {
    return preferences.getBoolean(getKey(R.string.p_widget_show_description), true);
  }

  boolean showFullDescription() {
    return preferences.getBoolean(getKey(R.string.p_widget_show_full_description), false);
  }

  boolean dueDateBelowTitle() {
    return preferences.getBoolean(getKey(R.string.p_widget_due_date_underneath), false);
  }

  int getWidgetSpacing() {
    int spacing = preferences.getIntegerFromString(getKey(R.string.p_widget_spacing), 0);
    if (spacing == 2) {
      return 0;
    }
    int dimen = spacing == 1 ? R.dimen.widget_padding_compact : R.dimen.widget_padding;
    return (int) context.getResources().getDimension(dimen);
  }

  int getFontSize() {
    return preferences.getInt(getKey(R.string.p_widget_font_size), 16);
  }

  public String getFilterId() {
    return preferences.getStringValue(getKey(R.string.p_widget_filter));
  }

  public int getThemeIndex() {
    return preferences.getInt(getKey(R.string.p_widget_theme), 0);
  }

  public int getColor() {
    int color = preferences.getInt(getKey(R.string.p_widget_color_v2), 0);
    if (color != 0) {
      return color;
    }
    int index = preferences.getInt(getKey(R.string.p_widget_color), -1);
    color = ContextCompat.getColor(context, Upgrader.getLegacyColor(index, R.color.blue_500));
    preferences.setInt(getKey(R.string.p_widget_color_v2), color);
    return color;
  }

  int getHeaderOpacity() {
    return getAlphaValue(R.string.p_widget_header_opacity);
  }

  int getRowOpacity() {
    return getAlphaValue(R.string.p_widget_opacity);
  }

  int getEmptySpaceOpacity() {
    return getAlphaValue(R.string.p_widget_empty_space_opacity);
  }

  private int getAlphaValue(int resId) {
    return (int) ((preferences.getInt(getKey(resId), 100) / 100.0) * 255.0);
  }

  public void setColor(int color) {
    preferences.setInt(getKey(R.string.p_widget_color_v2), color);
  }

  public void setTheme(int index) {
    preferences.setInt(getKey(R.string.p_widget_theme), index);
  }

  public String getKey(int resId) {
    return context.getString(resId) + widgetId;
  }

  public void setFilter(String filterPreferenceValue) {
    preferences.setString(getKey(R.string.p_widget_filter), filterPreferenceValue);
  }
}
