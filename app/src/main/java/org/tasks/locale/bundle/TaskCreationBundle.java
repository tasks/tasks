package org.tasks.locale.bundle;

import android.os.Bundle;
import org.tasks.BuildConfig;

public class TaskCreationBundle {

  public static final String EXTRA_BUNDLE = "org.tasks.locale.create";
  public static final String EXTRA_TITLE = "org.tasks.locale.create.STRING_TITLE";
  public static final String EXTRA_DUE_DATE = "org.tasks.locale.create.STRING_DUE_DATE";
  public static final String EXTRA_DUE_TIME = "org.tasks.locale.create.STRING_DUE_TIME";
  public static final String EXTRA_PRIORITY = "org.tasks.locale.create.STRING_PRIORITY";
  public static final String EXTRA_DESCRIPTION = "org.tasks.locale.create.STRING_DESCRIPTION";
  private static final String EXTRA_VERSION_CODE = "org.tasks.locale.create.INT_VERSION_CODE";

  private final Bundle bundle;

  public TaskCreationBundle() {
    this(null);
  }

  public TaskCreationBundle(Bundle bundle) {
    if (bundle == null) {
      this.bundle = new Bundle();
      this.bundle.putInt(EXTRA_VERSION_CODE, BuildConfig.VERSION_CODE);
    } else {
      this.bundle = bundle;
    }
  }

  public static boolean isBundleValid(Bundle bundle) {
    return -1 != bundle.getInt(EXTRA_VERSION_CODE, -1);
  }

  public String getTitle() {
    return bundle.getString(EXTRA_TITLE);
  }

  public void setTitle(String title) {
    bundle.putString(EXTRA_TITLE, title);
  }

  public String getDueDate() {
    return bundle.getString(EXTRA_DUE_DATE);
  }

  public void setDueDate(String dueDate) {
    bundle.putString(EXTRA_DUE_DATE, dueDate);
  }

  public String getDueTime() {
    return bundle.getString(EXTRA_DUE_TIME);
  }

  public void setDueTime(String dueTime) {
    bundle.putString(EXTRA_DUE_TIME, dueTime);
  }

  public String getPriority() {
    return bundle.getString(EXTRA_PRIORITY);
  }

  public void setPriority(String priority) {
    bundle.putString(EXTRA_PRIORITY, priority);
  }

  public String getDescription() {
    return bundle.getString(EXTRA_DESCRIPTION);
  }

  public void setDescription(String description) {
    bundle.putString(EXTRA_DESCRIPTION, description);
  }

  public Bundle build() {
    bundle.putInt(EXTRA_VERSION_CODE, BuildConfig.VERSION_CODE);
    return bundle;
  }

  @Override
  public String toString() {
    return "TaskCreationBundle{" + "bundle=" + bundle + '}';
  }
}
