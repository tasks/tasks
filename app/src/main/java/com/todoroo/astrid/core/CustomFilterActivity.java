/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.core;

import static android.text.TextUtils.isEmpty;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.mapToSerializedString;
import static java.util.Arrays.asList;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.UnaryCriterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.api.TextInputCriterion;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.FilterDao;
import org.tasks.databinding.CustomFilterActivityBinding;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.filters.FilterCriteriaProvider;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.locale.Locale;
import timber.log.Timber;

/**
 * Activity that allows users to build custom filters
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class CustomFilterActivity extends ThemedInjectingAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  static final int MENU_GROUP_CONTEXT_TYPE = 1;
  static final int MENU_GROUP_CONTEXT_DELETE = 2;
  private static final String EXTRA_CRITERIA = "extra_criteria";
  private static final int MENU_GROUP_FILTER = 0;

  @Inject Database database;
  @Inject FilterDao filterDao;
  @Inject DialogBuilder dialogBuilder;
  @Inject FilterCriteriaProvider filterCriteriaProvider;
  @Inject Locale locale;

  private CustomFilterActivityBinding binding;

  private ListView listView;
  private CustomFilterAdapter adapter;

  private static String serializeFilters(CustomFilterAdapter adapter) {
    List<String> rows = new ArrayList<>();
    for (int i = 0; i < adapter.getCount(); i++) {
      CriterionInstance item = adapter.getItem(i);
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

  private List<CriterionInstance> deserializeCriterion(@Nullable String criterion) {
    if (Strings.isNullOrEmpty(criterion)) {
      return Collections.emptyList();
    }
    List<CriterionInstance> entries = new ArrayList<>();
    for (String row : criterion.split("\n")) {
      CriterionInstance entry = new CriterionInstance();
      List<String> split =
          transform(
              Splitter.on(AndroidUtilities.SERIALIZATION_SEPARATOR).splitToList(row),
              CustomFilterActivity::unescape);
      if (split.size() != 4 && split.size() != 5) {
        Timber.e("invalid row: %s", row);
        return Collections.emptyList();
      }

      entry.criterion = filterCriteriaProvider.getFilterCriteria(split.get(0));
      String value = split.get(1);
      if (entry.criterion instanceof TextInputCriterion) {
        entry.selectedText = value;
      } else if (entry.criterion instanceof MultipleSelectCriterion) {
        MultipleSelectCriterion multipleSelectCriterion = (MultipleSelectCriterion) entry.criterion;
        if (multipleSelectCriterion.entryValues != null) {
          entry.selectedIndex = asList(multipleSelectCriterion.entryValues).indexOf(value);
        }
      } else {
        Timber.d("Ignored value %s for %s", value, entry.criterion);
      }
      entry.type = Integer.parseInt(split.get(3));
      entry.criterion.sql = split.get(4);
      Timber.d("%s -> %s", row, entry);
      entries.add(entry);
    }
    return entries;
  }

  private static String escape(String item) {
    if (item == null) {
      return ""; // $NON-NLS-1$
    }
    return item.replace(
        AndroidUtilities.SERIALIZATION_SEPARATOR, AndroidUtilities.SEPARATOR_ESCAPE);
  }

  private static String unescape(String item) {
    if (Strings.isNullOrEmpty(item)) {
      return "";
    }
    return item.replace(
        AndroidUtilities.SEPARATOR_ESCAPE, AndroidUtilities.SERIALIZATION_SEPARATOR);
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(EXTRA_CRITERIA, serializeFilters(adapter));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = CustomFilterActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    Toolbar toolbar = binding.toolbar.toolbar;
    toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_outline_clear_24px));
    toolbar.setTitle(R.string.FLA_new_filter);
    toolbar.inflateMenu(R.menu.menu_custom_filter_activity);
    toolbar.setOnMenuItemClickListener(this);
    toolbar.setNavigationOnClickListener(view -> discard());
    themeColor.apply(toolbar);
    listView = findViewById(android.R.id.list);

    List<CriterionInstance> criteria =
        new ArrayList<>(
            deserializeCriterion(
                savedInstanceState == null
                    ? getIntent().getStringExtra(EXTRA_CRITERIA)
                    : savedInstanceState.getString(EXTRA_CRITERIA)));
    if (criteria.isEmpty()) {
      CriterionInstance instance = new CriterionInstance();
      instance.criterion = filterCriteriaProvider.getStartingUniverse();
      instance.type = CriterionInstance.TYPE_UNIVERSE;
      criteria.add(instance);
    }
    adapter = new CustomFilterAdapter(this, dialogBuilder, criteria, locale);
    listView.setAdapter(adapter);
    updateList();

    setUpListeners();
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  private void setUpListeners() {
    findViewById(R.id.add).setOnClickListener(v -> listView.showContextMenu());

    listView.setOnCreateContextMenuListener(
        (menu, v, menuInfo) -> {
          if (menu.hasVisibleItems()) {
            /* If it has items already, then the user did not click on the "Add Criteria" button, but instead
              long held on a row in the list view, which caused CustomFilterAdapter.onCreateContextMenu
              to be invoked before this onCreateContextMenu method was invoked.
            */
            return;
          }

          int i = 0;
          for (CustomFilterCriterion item : filterCriteriaProvider.getAll()) {
            menu.add(CustomFilterActivity.MENU_GROUP_FILTER, i, 0, item.name);
            i++;
          }
        });
  }

  // --- listeners and action events

  @Override
  public void finish() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(binding.filterName.getWindowToken(), 0);
    super.finish();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    if (menu.size() > 0) {
      menu.clear();
    }

    // view holder
    if (v.getTag() != null) {
      adapter.onCreateContextMenu(menu, v);
    }
  }

  private void saveAndView() {
    String title = binding.filterName.getText().toString().trim();

    if (isEmpty(title)) {
      return;
    }

    StringBuilder sql = new StringBuilder(" WHERE ");
    Map<String, Object> values = new HashMap<>();
    for (int i = 0; i < adapter.getCount(); i++) {
      CriterionInstance instance = adapter.getItem(i);
      String value = instance.getValueFromCriterion();
      if (value == null && instance.criterion.sql != null && instance.criterion.sql.contains("?")) {
        value = "";
      }

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
        sql.append(TaskCriteria.activeAndVisible()).append(' ');
      } else {
        String subSql = instance.criterion.sql.replace("?", UnaryCriterion.sanitize(value));
        sql.append(Task.ID).append(" IN (").append(subSql).append(") ");
      }

      if (instance.criterion.valuesForNewTasks != null
          && instance.type == CriterionInstance.TYPE_INTERSECT) {
        for (Entry<String, Object> entry : instance.criterion.valuesForNewTasks.entrySet()) {
          values.put(
              entry.getKey().replace("?", value), entry.getValue().toString().replace("?", value));
        }
      }
    }

    org.tasks.data.Filter filter = persist(title, sql.toString(), values);
    Filter customFilter = new CustomFilter(filter);
    setResult(RESULT_OK, new Intent().putExtra(MainActivity.OPEN_FILTER, customFilter));
    finish();
  }

  /** Recalculate all sizes */
  void updateList() {
    int max = 0, last = -1;

    StringBuilder sql =
        new StringBuilder(Query.select(new CountProperty()).from(Task.TABLE).toString())
            .append(" WHERE ");

    for (int i = 0; i < adapter.getCount(); i++) {
      CriterionInstance instance = adapter.getItem(i);
      String value = instance.getValueFromCriterion();
      if (value == null && instance.criterion.sql != null && instance.criterion.sql.contains("?")) {
        value = "";
      }

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
        subSql = PermaSql.replacePlaceholdersForQuery(subSql);
        sql.append(Task.ID).append(" IN (").append(subSql).append(") ");
      }

      Cursor cursor = database.query(sql.toString(), null);
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

    for (int i = 0; i < adapter.getCount(); i++) {
      CriterionInstance instance = adapter.getItem(i);
      instance.max = max;
    }

    adapter.notifyDataSetInvalidated();
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.menu_save) {
      saveAndView();
      return true;
    }
    return onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {
    discard();
  }

  private void discard() {
    if (binding.filterName.getText().toString().trim().isEmpty() && adapter.getCount() <= 1) {
      finish();
    } else {
      dialogBuilder
          .newDialog(R.string.discard_changes)
          .setPositiveButton(R.string.keep_editing, null)
          .setNegativeButton(R.string.discard, (dialog, which) -> finish())
          .show();
    }
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    if (item.getGroupId() == MENU_GROUP_FILTER) {
      // give an initial value for the row before adding it
      CustomFilterCriterion criterion = filterCriteriaProvider.getAll().get(item.getItemId());
      final CriterionInstance instance = new CriterionInstance();
      instance.criterion = criterion;
      adapter.showOptionsFor(
          instance,
          () -> {
            adapter.add(instance);
            updateList();
          });
      return true;
    }

    // item type context item
    else if (item.getGroupId() == MENU_GROUP_CONTEXT_TYPE) {
      CriterionInstance instance = adapter.getItem(item.getOrder());
      instance.type = item.getItemId();
      updateList();
    }

    // delete context item
    else if (item.getGroupId() == MENU_GROUP_CONTEXT_DELETE) {
      CriterionInstance instance = adapter.getItem(item.getOrder());
      adapter.remove(instance);
      updateList();
    }

    return super.onContextItemSelected(item);
  }

  private org.tasks.data.Filter persist(String title, String sql, Map<String, Object> values) {
    if (title == null || title.length() == 0) {
      return null;
    }

    // if filter of this name exists, edit it
    org.tasks.data.Filter storeObject = filterDao.getByName(title);
    if (storeObject == null) {
      storeObject = new org.tasks.data.Filter();
    }

    // populate saved filter properties
    storeObject.setTitle(title);
    storeObject.setSql(sql);
    storeObject.setValues(values == null ? "" : mapToSerializedString(values));
    storeObject.setCriterion(serializeFilters(adapter));

    storeObject.setId(filterDao.insertOrUpdate(storeObject));
    return storeObject.getId() >= 0 ? storeObject : null;
  }
}
