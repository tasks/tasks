package org.tasks.tasklist;

import android.support.v7.app.WindowDecorActionBar;
import android.support.v7.view.StandaloneActionMode;
import android.support.v7.widget.ActionBarContextView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.reflect.Field;
import org.tasks.R;
import org.tasks.themes.ThemeColor;

public class ActionUtils {

  // cribbed from Twittnuker
  public static void applySupportActionModeColor(
      ThemeColor themeColor, final android.support.v7.view.ActionMode modeCompat) {
    // Very dirty implementation
    // This call ensures TitleView created
    modeCompat.setTitle(modeCompat.getTitle());
    View contextView = null;
    if (modeCompat instanceof WindowDecorActionBar.ActionModeImpl) {
      WindowDecorActionBar actionBar =
          (WindowDecorActionBar)
              findFieldOfTypes(
                  modeCompat,
                  WindowDecorActionBar.ActionModeImpl.class,
                  WindowDecorActionBar.class);
      if (actionBar == null) {
        return;
      }
      contextView =
          (View)
              findFieldOfTypes(actionBar, WindowDecorActionBar.class, ActionBarContextView.class);
    } else if (modeCompat instanceof StandaloneActionMode) {
      contextView =
          (View)
              findFieldOfTypes(modeCompat, StandaloneActionMode.class, ActionBarContextView.class);
    }
    if (!(contextView instanceof ActionBarContextView)) {
      return;
    }

    contextView.setBackgroundColor(themeColor.getPrimaryColor());

    TextView title = contextView.findViewById(R.id.action_bar_title);
    if (title != null) {
      title.setTextColor(themeColor.getActionBarTint());
    }

    ImageView closeButton = contextView.findViewById(R.id.action_mode_close_button);
    if (closeButton != null) {
      closeButton.setColorFilter(themeColor.getActionBarTint());
    }
  }

  private static <T> Object findFieldOfTypes(
      T obj, Class<? extends T> cls, Class<?>... checkTypes) {
    labelField:
    for (Field field : cls.getDeclaredFields()) {
      field.setAccessible(true);
      final Object fieldObj;
      try {
        fieldObj = field.get(obj);
      } catch (Exception ignore) {
        continue;
      }
      if (fieldObj != null) {
        final Class<?> type = fieldObj.getClass();
        for (Class<?> checkType : checkTypes) {
          if (!checkType.isAssignableFrom(type)) {
            continue labelField;
          }
        }
        return fieldObj;
      }
    }
    return null;
  }
}
