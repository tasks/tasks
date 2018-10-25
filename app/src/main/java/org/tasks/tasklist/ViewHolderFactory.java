package org.tasks.tasklist;

import static androidx.core.content.ContextCompat.getColor;
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
import org.tasks.injection.ForActivity;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxes;
import org.tasks.ui.ChipProvider;

public class ViewHolderFactory {

  private final int textColorSecondary;
  private final int textColorOverdue;
  private final Context context;
  private final CheckBoxes checkBoxes;
  private final ChipProvider chipProvider;
  private final int fontSize;
  private final TaskDao taskDao;
  private final DisplayMetrics metrics;
  private final int background;
  private final int selectedColor;
  private final int rowPadding;
  private final Preferences preferences;

  @Inject
  public ViewHolderFactory(
      @ForActivity Context context,
      Preferences preferences,
      CheckBoxes checkBoxes,
      ChipProvider chipProvider,
      TaskDao taskDao) {
    this.context = context;
    this.checkBoxes = checkBoxes;
    this.chipProvider = chipProvider;
    this.taskDao = taskDao;
    this.preferences = preferences;
    textColorSecondary = getData(context, android.R.attr.textColorSecondary);
    textColorOverdue = getColor(context, R.color.overdue);
    background = getResourceId(context, R.attr.selectableItemBackground);
    selectedColor = getData(context, R.attr.colorControlHighlight);
    fontSize = preferences.getFontSize();
    metrics = context.getResources().getDisplayMetrics();
    rowPadding = convertDpToPixels(metrics, preferences.getInt(R.string.p_rowPadding, 16));
  }

  ViewHolder newViewHolder(ViewGroup parent, ViewHolder.ViewHolderCallbacks callbacks) {
    return new ViewHolder(
        context,
        (ViewGroup)
            LayoutInflater.from(context).inflate(R.layout.task_adapter_row_simple, parent, false),
        preferences,
        fontSize,
        checkBoxes,
        chipProvider,
        textColorOverdue,
        textColorSecondary,
        taskDao,
        callbacks,
        metrics,
        background,
        selectedColor,
        rowPadding);
  }
}
