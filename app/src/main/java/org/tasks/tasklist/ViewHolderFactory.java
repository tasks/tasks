package org.tasks.tasklist;

import static androidx.core.content.ContextCompat.getColor;
import static com.todoroo.andlib.utility.AndroidUtilities.convertDpToPixels;
import static org.tasks.preferences.ResourceResolver.getData;
import static org.tasks.preferences.ResourceResolver.getResourceId;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.todoroo.astrid.service.TaskCompleter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.Linkify;
import org.tasks.injection.ForActivity;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxProvider;
import org.tasks.ui.ChipProvider;

public class ViewHolderFactory {

  private final int textColorSecondary;
  private final int textColorOverdue;
  private final Context context;
  private final ChipProvider chipProvider;
  private final int fontSize;
  private final CheckBoxProvider checkBoxProvider;
  private final TaskCompleter taskCompleter;
  private final DisplayMetrics metrics;
  private final int background;
  private final int selectedColor;
  private final int rowPadding;
  private final Linkify linkify;
  private final Preferences preferences;

  @Inject
  public ViewHolderFactory(
      @ForActivity Context context,
      Preferences preferences,
      ChipProvider chipProvider,
      CheckBoxProvider checkBoxProvider,
      TaskCompleter taskCompleter,
      Linkify linkify) {
    this.context = context;
    this.chipProvider = chipProvider;
    this.checkBoxProvider = checkBoxProvider;
    this.taskCompleter = taskCompleter;
    this.preferences = preferences;
    this.linkify = linkify;
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
        (Activity) context,
        (ViewGroup)
            LayoutInflater.from(context).inflate(R.layout.task_adapter_row, parent, false),
        preferences,
        fontSize,
        chipProvider,
        checkBoxProvider,
        textColorOverdue,
        textColorSecondary,
        taskCompleter,
        callbacks,
        metrics,
        background,
        selectedColor,
        rowPadding,
        linkify);
  }
}
