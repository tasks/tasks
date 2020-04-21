/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package org.tasks.activities;

import static android.text.TextUtils.isEmpty;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.UnaryCriterion;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.core.CriterionInstance;
import com.todoroo.astrid.core.CustomFilterAdapter;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.FilterDao;
import org.tasks.filters.FilterCriteriaProvider;
import org.tasks.injection.ActivityComponent;
import org.tasks.locale.Locale;

public class FilterSettingsActivity extends BaseListSettingsActivity {

  public static final int MENU_GROUP_CONTEXT_TYPE = 1;
  public static final int MENU_GROUP_CONTEXT_DELETE = 2;
  private static final int MENU_GROUP_FILTER = 0;

  public static final String TOKEN_FILTER = "token_filter";
  public static final String EXTRA_CRITERIA = "extra_criteria";

  @Inject FilterDao filterDao;
  @Inject Locale locale;
  @Inject Database database;
  @Inject FilterCriteriaProvider filterCriteriaProvider;

  @BindView(R.id.name)
  TextInputEditText name;

  @BindView(R.id.name_layout)
  TextInputLayout nameLayout;

  @BindView(R.id.list)
  ListView listView; // TODO: convert to recycler view

  @BindView(R.id.fab)
  ExtendedFloatingActionButton fab;

  private CustomFilter filter;
  private CustomFilterAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    filter = getIntent().getParcelableExtra(TOKEN_FILTER);
    if (filter == null) {
      org.tasks.data.Filter f = new org.tasks.data.Filter();
      f.setSql("");
      this.filter = new CustomFilter(f);
    }

    super.onCreate(savedInstanceState);

    if (savedInstanceState == null && filter != null) {
      selectedColor = filter.tint;
      selectedIcon = filter.icon;
      name.setText(filter.listingTitle);
    }

    List<CriterionInstance> criteria =
        new ArrayList<>(
            CriterionInstance.fromString(
                filterCriteriaProvider,
                savedInstanceState == null
                    ? filter.getCriterion()
                    : savedInstanceState.getString(EXTRA_CRITERIA)));
    if (criteria.isEmpty()) {
      CriterionInstance instance = new CriterionInstance();
      instance.criterion = filterCriteriaProvider.getStartingUniverse();
      instance.type = CriterionInstance.TYPE_UNIVERSE;
      criteria.add(instance);
    }
    adapter = new CustomFilterAdapter(this, dialogBuilder, criteria, locale);
    fab.setExtended(adapter.getCount() <= 1);
    listView.setAdapter(adapter);

    updateList();

    setUpListeners();

    updateTheme();
  }

  @OnClick(R.id.fab)
  void addCriteria() {
    listView.showContextMenu();
    fab.shrink();
  }

  private void setUpListeners() {
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
            menu.add(MENU_GROUP_FILTER, i, 0, item.name);
            i++;
          }
        });
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(EXTRA_CRITERIA, adapter.getCriterion());
  }

  @Override
  protected boolean isNew() {
    return filter.getId() == 0;
  }

  @Override
  protected String getToolbarTitle() {
    return isNew() ? getString(R.string.FLA_new_filter) : filter.listingTitle;
  }

  @OnTextChanged(R.id.name)
  void onTextChanged() {
    nameLayout.setError(null);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  protected void save() {
    String newName = getNewName();

    if (isEmpty(newName)) {
      nameLayout.setError(getString(R.string.name_cannot_be_empty));
      return;
    }

    if (hasChanges()) {
      filter.listingTitle = newName;
      filter.tint = selectedColor;
      filter.icon = selectedIcon;
      filter.sqlQuery = adapter.getSql();
      filter.valuesForNewTasks.clear();
      for (Map.Entry<String, Object> entry : adapter.getValues().entrySet()) {
        filter.valuesForNewTasks.put(entry.getKey(), entry.getValue());
      }
      filter.setCriterion(adapter.getCriterion());
      filterDao.insertOrUpdate(filter.toStoreObject());
      setResult(
          RESULT_OK,
          new Intent(TaskListFragment.ACTION_RELOAD).putExtra(MainActivity.OPEN_FILTER, filter));
    }

    finish();
  }

  private String getNewName() {
    return name.getText().toString().trim();
  }

  @Override
  protected boolean hasChanges() {
    return !(getNewName().equals(filter.listingTitle)
        && selectedColor == filter.tint
        && selectedIcon == filter.icon
        && adapter.getSql().equals(filter.sqlQuery)
        && adapter.getValues().equals(filter.valuesForNewTasks)
        && adapter.getCriterion().equals(filter.getCriterion()));
  }

  @Override
  public void finish() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(name.getWindowToken(), 0);
    super.finish();
  }

  @Override
  protected int getLayout() {
    return R.layout.filter_settings_activity;
  }

  @Override
  protected void delete() {
    filterDao.delete(filter.getId());
    setResult(
        RESULT_OK,
        new Intent(TaskListFragment.ACTION_DELETED).putExtra(TOKEN_FILTER, filter));
    finish();
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

  public void updateList() {
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
}
