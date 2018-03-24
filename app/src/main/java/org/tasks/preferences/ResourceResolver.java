package org.tasks.preferences;

import android.content.Context;
import android.util.TypedValue;

public class ResourceResolver {

  @Deprecated
  public static int getData(Context context, int attr) {
    TypedValue typedValue = new TypedValue();
    context.getTheme().resolveAttribute(attr, typedValue, true);
    return typedValue.data;
  }

  @Deprecated
  public static float getDimen(Context context, int resId) {
    TypedValue typedValue = new TypedValue();
    context.getResources().getValue(resId, typedValue, true);
    return typedValue.getFloat();
  }

  @Deprecated
  public static int getResourceId(Context context, int attr) {
    TypedValue typedValue = new TypedValue();
    context.getTheme().resolveAttribute(attr, typedValue, true);
    return typedValue.resourceId;
  }
}
