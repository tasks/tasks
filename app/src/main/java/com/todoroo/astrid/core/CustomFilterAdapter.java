/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.core;

import static com.todoroo.astrid.core.CriterionInstance.escape;
import static java.util.Arrays.asList;

import android.content.DialogInterface;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.common.base.Joiner;
import com.todoroo.andlib.sql.UnaryCriterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.api.TextInputCriterion;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.tasks.R;
import org.tasks.activities.FilterSettingsActivity;
import org.tasks.dialogs.AlertDialogBuilder;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.locale.Locale;
import timber.log.Timber;

/**
 * Adapter for AddOns
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class CustomFilterAdapter extends ArrayAdapter<CriterionInstance> {

  private final FilterSettingsActivity activity;
  private final DialogBuilder dialogBuilder;
  private final LayoutInflater inflater;
  private final Locale locale;
  private final View.OnClickListener filterClickListener =
      new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          ViewHolder viewHolder = (ViewHolder) v.getTag();
          if (viewHolder == null) {
            return;
          }
          if (viewHolder.item.type == CriterionInstance.TYPE_UNIVERSE) {
            return;
          }

          showOptionsFor(
              viewHolder.item,
              () -> {
                activity.updateList();
                notifyDataSetInvalidated();
              });
        }
      };

  // --- view event handling

  public CustomFilterAdapter(
      FilterSettingsActivity activity,
      DialogBuilder dialogBuilder,
      List<CriterionInstance> objects,
      Locale locale) {
    super(activity, 0, objects);
    this.activity = activity;
    this.dialogBuilder = dialogBuilder;
    this.locale = locale;
    inflater = activity.getLayoutInflater();
  }

  public void onCreateContextMenu(ContextMenu menu, View v) {
    // view holder
    ViewHolder viewHolder = (ViewHolder) v.getTag();
    if (viewHolder == null || viewHolder.item.type == CriterionInstance.TYPE_UNIVERSE) {
      return;
    }

    int index = getPosition(viewHolder.item);

    menu.setHeaderTitle(viewHolder.name.getText());

    MenuItem item =
        menu.add(
            FilterSettingsActivity.MENU_GROUP_CONTEXT_TYPE,
            CriterionInstance.TYPE_INTERSECT,
            index,
            activity.getString(
                R.string.CFA_context_chain, activity.getString(R.string.CFA_type_intersect)));
    item.setChecked(viewHolder.item.type == CriterionInstance.TYPE_INTERSECT);
    item =
        menu.add(
            FilterSettingsActivity.MENU_GROUP_CONTEXT_TYPE,
            CriterionInstance.TYPE_ADD,
            index,
            activity.getString(
                R.string.CFA_context_chain, activity.getString(R.string.CFA_type_add)));
    item.setChecked(viewHolder.item.type == CriterionInstance.TYPE_ADD);

    item =
        menu.add(
            FilterSettingsActivity.MENU_GROUP_CONTEXT_TYPE,
            CriterionInstance.TYPE_SUBTRACT,
            index,
            activity.getString(
                R.string.CFA_context_chain, activity.getString(R.string.CFA_type_subtract)));
    item.setChecked(viewHolder.item.type == CriterionInstance.TYPE_SUBTRACT);
    menu.setGroupCheckable(FilterSettingsActivity.MENU_GROUP_CONTEXT_TYPE, true, true);

    menu.add(FilterSettingsActivity.MENU_GROUP_CONTEXT_DELETE, 0, index, R.string.CFA_context_delete);
  }

  /** Show options menu for the given criterioninstance */
  public void showOptionsFor(final CriterionInstance item, final Runnable onComplete) {
    AlertDialogBuilder dialog = dialogBuilder.newDialog(item.criterion.name);

    if (item.criterion instanceof MultipleSelectCriterion) {
      MultipleSelectCriterion multiSelectCriterion = (MultipleSelectCriterion) item.criterion;
      final String[] titles = multiSelectCriterion.entryTitles;
      DialogInterface.OnClickListener listener =
          (click, which) -> {
            item.selectedIndex = which;
            if (onComplete != null) {
              onComplete.run();
            }
          };
      dialog.setItems(titles, listener);
    } else if (item.criterion instanceof TextInputCriterion) {
      TextInputCriterion textInCriterion = (TextInputCriterion) item.criterion;
      FrameLayout frameLayout = new FrameLayout(activity);
      frameLayout.setPadding(10, 0, 10, 0);
      final EditText editText = new EditText(activity);
      editText.setText(item.selectedText);
      editText.setHint(textInCriterion.hint);
      frameLayout.addView(
          editText,
          new FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
      dialog
          .setView(frameLayout)
          .setPositiveButton(
              android.R.string.ok,
              (dialogInterface, which) -> {
                item.selectedText = editText.getText().toString();
                if (onComplete != null) {
                  onComplete.run();
                }
              });
    }

    dialog.show().setOwnerActivity(activity);
  }

  // --- view construction

  @NonNull
  @Override
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.custom_filter_row, parent, false);
      ViewHolder viewHolder = new ViewHolder();
      viewHolder.type = convertView.findViewById(R.id.type);
      viewHolder.name = convertView.findViewById(R.id.name);
      viewHolder.filterCount = convertView.findViewById(R.id.filter_count);
      convertView.setTag(viewHolder);
    }

    ViewHolder viewHolder = (ViewHolder) convertView.getTag();
    viewHolder.item = getItem(position);
    initializeView(convertView);

    // listeners
    convertView.setOnCreateContextMenuListener(activity);
    convertView.setOnClickListener(filterClickListener);

    return convertView;
  }

  private void initializeView(View convertView) {
    ViewHolder viewHolder = (ViewHolder) convertView.getTag();
    CriterionInstance item = viewHolder.item;

    String title = item.getTitleFromCriterion();

    viewHolder.type.setVisibility(
        item.type == CriterionInstance.TYPE_UNIVERSE ? View.GONE : View.VISIBLE);
    switch (item.type) {
      case CriterionInstance.TYPE_ADD:
        viewHolder.type.setImageResource(R.drawable.arrow_join);
        title = activity.getString(R.string.CFA_type_add) + " " + title;
        break;
      case CriterionInstance.TYPE_SUBTRACT:
        viewHolder.type.setImageResource(R.drawable.arrow_branch);
        title = activity.getString(R.string.CFA_type_subtract) + " " + title;
        break;
      case CriterionInstance.TYPE_INTERSECT:
        viewHolder.type.setImageResource(R.drawable.arrow_down);
        break;
    }

    viewHolder.name.setText(title);
    viewHolder.filterCount.setText(locale.formatNumber(item.end));
  }

  private String getValue(CriterionInstance instance) {
    String value = instance.getValueFromCriterion();
    if (value == null && instance.criterion.sql != null && instance.criterion.sql.contains("?")) {
      value = "";
    }
    return value;
  }

  public String getSql() {
    StringBuilder sql = new StringBuilder(" WHERE ");
    for (int i = 0; i < getCount(); i++) {
      CriterionInstance instance = getItem(i);
      String value = getValue(instance);

      switch (instance.type) {
        case CriterionInstance.TYPE_ADD:
          sql.append("OR ");
          break;
        case CriterionInstance.TYPE_SUBTRACT:
          sql.append("AND NOT ");
          break;
        case CriterionInstance.TYPE_INTERSECT:
          sql.append("AND ");
          break;
      }

      // special code for all tasks universe
      if (instance.type == CriterionInstance.TYPE_UNIVERSE || instance.criterion.sql == null) {
        sql.append(TaskCriteria.activeAndVisible()).append(' ');
      } else {
        String subSql = instance.criterion.sql.replace("?", UnaryCriterion.sanitize(value));
        sql.append(Task.ID).append(" IN (").append(subSql).append(") ");
      }
    }
    return sql.toString();
  }

  public Map<String, Object> getValues() {
    Map<String, Object> values = new HashMap<>();
    for (int i = 0; i < getCount(); i++) {
      CriterionInstance instance = getItem(i);
      String value = getValue(instance);

      if (instance.criterion.valuesForNewTasks != null
          && instance.type == CriterionInstance.TYPE_INTERSECT) {
        for (Entry<String, Object> entry : instance.criterion.valuesForNewTasks.entrySet()) {
          values.put(
              entry.getKey().replace("?", value), entry.getValue().toString().replace("?", value));
        }
      }
    }
    return values;
  }

  public String getCriterion() {
    List<String> rows = new ArrayList<>();
    for (int i = 0; i < getCount(); i++) {
      CriterionInstance item = getItem(i);
      // criterion|entry|text|type|sql
      String row =
          Joiner.on(AndroidUtilities.SERIALIZATION_SEPARATOR)
              .join(
                  asList(
                      escape(item.criterion.identifier),
                      escape(item.getValueFromCriterion()),
                      escape(item.criterion.text),
                      item.type,
                      item.criterion.sql == null ? "" : item.criterion.sql));
      Timber.d("%s -> %s", item, row);
      rows.add(row);
    }
    return Joiner.on("\n").join(rows);
  }

  private static class ViewHolder {
    CriterionInstance item;
    ImageView type;
    TextView name;
    TextView filterCount;
  }
}
