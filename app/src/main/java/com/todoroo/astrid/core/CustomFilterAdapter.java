/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.DialogInterface;
import androidx.annotation.NonNull;

import android.database.Cursor;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.UnaryCriterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.api.TextInputCriterion;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.core.CustomFilterActivity.CriterionInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.tasks.R;
import org.tasks.dialogs.AlertDialogBuilder;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.filters.FilterCriteriaProvider;
import org.tasks.locale.Locale;

/**
 * Adapter for AddOns
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class CustomFilterAdapter extends ArrayAdapter<CriterionInstance> {

  private static final String IDENTIFIER_UNIVERSE = "active"; // $NON-NLS-1$
  private static final String CRITERION_SEPARATOR = "\n";

  private final CustomFilterActivity activity;
  private final Database database;
  private final FilterCriteriaProvider filterCriteriaProvider;
  private final DialogBuilder dialogBuilder;
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
                updateList();
              });
        }
      };

  // --- view event handling

  public CustomFilterAdapter(
      CustomFilterActivity activity,
      Database database,
      FilterCriteriaProvider filterCriteriaProvider,
      DialogBuilder dialogBuilder,
      Locale locale) {
    super(activity, 0);
    this.activity = activity;
    this.database = database;
    this.filterCriteriaProvider = filterCriteriaProvider;
    this.dialogBuilder = dialogBuilder;
    this.locale = locale;
    addAll(getStartingUniverse());
    updateList();
  }

  public CustomFilterAdapter(
          CustomFilterActivity activity,
          Database database,
          FilterCriteriaProvider filterCriteriaProvider,
          String serializedFilters,
          DialogBuilder dialogBuilder,
          Locale locale) {
    super(activity, 0);
    this.activity = activity;
    this.database = database;
    this.filterCriteriaProvider = filterCriteriaProvider;
    this.dialogBuilder = dialogBuilder;
    this.locale = locale;
    addAll(deserializeFilters(serializedFilters));
    updateList();
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
            CustomFilterActivity.MENU_GROUP_CONTEXT_TYPE,
            CriterionInstance.TYPE_INTERSECT,
            index,
            activity.getString(
                R.string.CFA_context_chain, activity.getString(R.string.CFA_type_intersect)));
    item.setChecked(viewHolder.item.type == CriterionInstance.TYPE_INTERSECT);
    item =
        menu.add(
            CustomFilterActivity.MENU_GROUP_CONTEXT_TYPE,
            CriterionInstance.TYPE_ADD,
            index,
            activity.getString(
                R.string.CFA_context_chain, activity.getString(R.string.CFA_type_add)));
    item.setChecked(viewHolder.item.type == CriterionInstance.TYPE_ADD);

    item =
        menu.add(
            CustomFilterActivity.MENU_GROUP_CONTEXT_TYPE,
            CriterionInstance.TYPE_SUBTRACT,
            index,
            activity.getString(
                R.string.CFA_context_chain, activity.getString(R.string.CFA_type_subtract)));
    item.setChecked(viewHolder.item.type == CriterionInstance.TYPE_SUBTRACT);
    menu.setGroupCheckable(CustomFilterActivity.MENU_GROUP_CONTEXT_TYPE, true, true);

    menu.add(CustomFilterActivity.MENU_GROUP_CONTEXT_DELETE, 0, index, R.string.CFA_context_delete);
  }

  /** Show options menu for the given criterioninstance */
  public void showOptionsFor(final CriterionInstance item, final Runnable onComplete) {
    AlertDialogBuilder dialog = dialogBuilder.newDialog().setTitle(item.criterion.name);

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
      convertView = activity.getLayoutInflater().inflate(R.layout.custom_filter_row, parent, false);
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

  public String serializeFilters() {
    StringBuilder values = new StringBuilder();
    for (int i = 0; i < getCount(); i++) {
      CriterionInstance item = getItem(i);

      // criterion|entry|text|type|sql
      values.append(escape(item.criterion.identifier));
      values.append(AndroidUtilities.SERIALIZATION_SEPARATOR);
      values.append(escape(getValueFromCriterion(item)));
      values.append(AndroidUtilities.SERIALIZATION_SEPARATOR);
      values.append(escape(item.criterion.text));
      values.append(AndroidUtilities.SERIALIZATION_SEPARATOR);
      values.append(item.type);
      values.append(AndroidUtilities.SERIALIZATION_SEPARATOR);
      if (item.criterion.sql != null) {
        values.append(item.criterion.sql);
      }
      values.append(CRITERION_SEPARATOR);
    }
    return values.toString();
  }

  public List<CriterionInstance> deserializeFilters(String serializedFilters) {
    List<CriterionInstance> result = new ArrayList<>();
    result.add(getStartingUniverse());
    if (serializedFilters!=null) {
      String[] criterionLines = serializedFilters.split(CRITERION_SEPARATOR);
      for(String criterionLine: criterionLines) {
        String[] values = criterionLine.split("\\" + AndroidUtilities.SERIALIZATION_SEPARATOR);
        if (values.length>=4 && !IDENTIFIER_UNIVERSE.equals(values[0])) {
          CriterionInstance instance = new CriterionInstance();
          String criterionIdentifier = values[0];
          instance.criterion = filterCriteriaProvider.getByIdentifier(criterionIdentifier);
          String criterionText = values[1];
          if (instance.criterion instanceof MultipleSelectCriterion) {
            if (criterionText.equals(instance.criterion.text)) {
              // Nothing to do
            } else {
              String[] entryValues = ((MultipleSelectCriterion) instance.criterion).entryValues;
              for(int i=0;  i < entryValues.length; i++) {
                if (criterionText.equals(entryValues[i])) {
                  instance.selectedIndex = i;
                }
              }
            }
          } else if (instance.criterion instanceof TextInputCriterion) {
            instance.selectedText = criterionText;
          }
          instance.type = Integer.parseInt(values[3]);
          result.add(instance);
        }
      }
    }
    return result;
  }

  public void addItem(int itemNr) {
    final CriterionInstance instance = new CriterionInstance();
    instance.criterion = filterCriteriaProvider.getAll().get(itemNr);
    showOptionsFor(
            instance,
            () -> {
              add(instance);
              updateList();
            });
  }

  public void removeItem(int itemNr) {
    CriterionInstance instance = getItem(itemNr);
    remove(instance);
    updateList();
  }

  public void setItemType(int itemNr, int itemType) {
    CriterionInstance instance = getItem(itemNr);
    instance.type = itemType;
    updateList();
  }

  public String toSql(Map<String, Object> values) {
    StringBuilder sql = new StringBuilder(" WHERE ");
    for (int i = 0; i < getCount(); i++) {
      CriterionInstance instance = getItem(i);

      sql.append(toSql(instance, false));

      String value = getValue(instance);
      if (instance.criterion.valuesForNewTasks != null
              && instance.type == CriterionInstance.TYPE_INTERSECT) {
        for (Map.Entry<String, Object> entry : instance.criterion.valuesForNewTasks.entrySet()) {
          values.put(
                  entry.getKey().replace("?", value), entry.getValue().toString().replace("?", value));
        }
      }
    }
    return sql.toString();
  }

  /** Recalculate all sizes */
  private void updateList() {
    int max = 0, last = -1;

    StringBuilder sql =
            new StringBuilder(Query.select(new Property.CountProperty()).from(Task.TABLE).toString())
                    .append(" WHERE ");

    for (int i = 0; i < getCount(); i++) {
      CriterionInstance instance = getItem(i);
      sql.append(toSql(instance, true));

      Cursor cursor = database.rawQuery(sql.toString());
      try {
        cursor.moveToNext();
        instance.start = last == -1 ? cursor.getInt(0) : last;
        instance.end = cursor.getInt(0);
        last = instance.end;
        max = Math.max(max, last);
      } finally {
        cursor.close();
      }
    }

    for (int i = 0; i < getCount(); i++) {
      CriterionInstance instance = getItem(i);
      instance.max = max;
    }

    notifyDataSetInvalidated();
  }

  private String toSql(CriterionInstance instance, boolean replacePlaceholders) {
    StringBuilder sql = new StringBuilder();
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
      case CriterionInstance.TYPE_UNIVERSE:
    }

    // special code for all tasks universe
    if (instance.criterion.sql == null) {
      sql.append(TaskDao.TaskCriteria.activeAndVisible()).append(' ');
    } else {
      String subSql = instance.criterion.sql.replace("?", UnaryCriterion.sanitize(value));
      if (replacePlaceholders) {
        subSql = PermaSql.replacePlaceholdersForQuery(subSql);
      }
      sql.append(Task.ID).append(" IN (").append(subSql).append(") ");
    }
    return sql.toString();
  }

  private String getValue(CriterionInstance instance) {
    String value = getValueFromCriterion(instance);
    if (value == null && instance.criterion.sql != null && instance.criterion.sql.contains("?")) {
      value = "";
    }
    return value;
  }

  private static String escape(String item) {
    if (item == null) {
      return ""; // $NON-NLS-1$
    }
    return item.replace(
            AndroidUtilities.SERIALIZATION_SEPARATOR, AndroidUtilities.SEPARATOR_ESCAPE);
  }

  private CriterionInstance getStartingUniverse() {
    CriterionInstance instance = new CriterionInstance();
    instance.criterion =
            new MultipleSelectCriterion(
                    IDENTIFIER_UNIVERSE,
                    activity.getString(R.string.CFA_universe_all),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
    instance.type = CriterionInstance.TYPE_UNIVERSE;
    return instance;
  }

  private void initializeView(View convertView) {
    ViewHolder viewHolder = (ViewHolder) convertView.getTag();
    CriterionInstance item = viewHolder.item;

    String title = getTitleFromCriterion(item);

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

  private String getTitleFromCriterion(CriterionInstance instance) {
    if (instance.criterion instanceof MultipleSelectCriterion) {
      if (instance.selectedIndex >= 0
              && ((MultipleSelectCriterion) instance.criterion).entryTitles != null
              && instance.selectedIndex < ((MultipleSelectCriterion) instance.criterion).entryTitles.length) {
        String title = ((MultipleSelectCriterion) instance.criterion).entryTitles[instance.selectedIndex];
        return instance.criterion.text.replace("?", title);
      }
      return instance.criterion.text;
    } else if (instance.criterion instanceof TextInputCriterion) {
      if (instance.selectedText == null) {
        return instance.criterion.text;
      }
      return instance.criterion.text.replace("?", instance.selectedText);
    }
    throw new UnsupportedOperationException("Unknown criterion type"); // $NON-NLS-1$
  }

  private String getValueFromCriterion(CriterionInstance instance) {
    if (instance.type == CriterionInstance.TYPE_UNIVERSE) {
      return null;
    }
    if (instance.criterion instanceof MultipleSelectCriterion) {
      if (instance.selectedIndex >= 0
              && ((MultipleSelectCriterion) instance.criterion).entryValues != null
              && instance.selectedIndex < ((MultipleSelectCriterion) instance.criterion).entryValues.length) {
        return ((MultipleSelectCriterion) instance.criterion).entryValues[instance.selectedIndex];
      }
      return instance.criterion.text;
    } else if (instance.criterion instanceof TextInputCriterion) {
      return instance.selectedText;
    }
    throw new UnsupportedOperationException("Unknown criterion type"); // $NON-NLS-1$
  }

  private class ViewHolder {
    CriterionInstance item;
    ImageView type;
    TextView name;
    TextView filterCount;
  }

}
