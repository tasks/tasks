package org.tasks.tasklist;

import static android.support.v4.content.ContextCompat.getColor;
import static com.todoroo.andlib.utility.AndroidUtilities.convertDpToPixels;
import static org.tasks.preferences.ResourceResolver.getData;
import static org.tasks.preferences.ResourceResolver.getResourceId;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.todoroo.astrid.dao.TaskDao;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxes;

public class ViewHolderFactory {

  private final int textColorSecondary;
  private final int textColorHint;
  private final int textColorOverdue;
  private final Context context;
  private final CheckBoxes checkBoxes;
  private final TagFormatter tagFormatter;
  private final boolean showFullTaskTitle;
  private final int fontSize;
  private final TaskDao taskDao;
  private final DialogBuilder dialogBuilder;
  private final DisplayMetrics metrics;
  private final int background;
  private final int selectedColor;
  private final int rowPadding;

  @Inject
  public ViewHolderFactory(
      @ForActivity Context context,
      Preferences preferences,
      CheckBoxes checkBoxes,
      TagFormatter tagFormatter,
      TaskDao taskDao,
      DialogBuilder dialogBuilder) {
    this.context = context;
    this.checkBoxes = checkBoxes;
    this.tagFormatter = tagFormatter;
    this.taskDao = taskDao;
    this.dialogBuilder = dialogBuilder;
    textColorSecondary = getData(context, android.R.attr.textColorSecondary);
    textColorHint = getData(context, android.R.attr.textColorTertiary);
    textColorOverdue = getColor(context, R.color.overdue);
    background = getResourceId(context, R.attr.selectableItemBackground);
    selectedColor = getData(context, R.attr.colorControlHighlight);
    showFullTaskTitle = preferences.getBoolean(R.string.p_fullTaskTitle, false);
    fontSize = preferences.getFontSize();
    metrics = context.getResources().getDisplayMetrics();
    rowPadding = convertDpToPixels(metrics, preferences.getInt(R.string.p_rowPadding, 16));
  }

  ViewHolder newViewHolder(ViewGroup parent, ViewHolder.ViewHolderCallbacks callbacks) {
    return new ViewHolder(
        context,
        (ViewGroup)
            LayoutInflater.from(context).inflate(R.layout.task_adapter_row_simple, parent, false),
        showFullTaskTitle,
        fontSize,
        checkBoxes,
        tagFormatter,
        textColorOverdue,
        textColorSecondary,
        textColorHint,
        taskDao,
        dialogBuilder,
        callbacks,
        metrics,
        background,
        selectedColor,
        rowPadding);
  }
}
