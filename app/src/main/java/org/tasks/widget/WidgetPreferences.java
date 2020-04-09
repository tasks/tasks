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

  public int getOpacity() {
    return preferences.getInt(getKey(R.string.p_widget_opacity), 100);
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
