/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package org.tasks.activities;

import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.mapToSerializedString;
import static org.tasks.Strings.isNullOrEmpty;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.UnaryCriterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.api.TextInputCriterion;
import com.todoroo.astrid.core.CriterionInstance;
import com.todoroo.astrid.core.CustomFilterAdapter;
import com.todoroo.astrid.core.CustomFilterItemTouchHelper;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.FilterDao;
import org.tasks.dialogs.AlertDialogBuilder;
import org.tasks.filters.FilterCriteriaProvider;
import org.tasks.injection.ActivityComponent;
import org.tasks.locale.Locale;

public class FilterSettingsActivity extends BaseListSettingsActivity {

  public static final String TOKEN_FILTER = "token_filter";
  public static final String EXTRA_TITLE = "extra_title";
  public static final String EXTRA_CRITERIA = "extra_criteria";
  @Inject FilterDao filterDao;
  @Inject Locale locale;
  @Inject Database database;
  @Inject FilterCriteriaProvider filterCriteriaProvider;
  private List<CriterionInstance> criteria;

  @BindView(R.id.name)
  TextInputEditText name;

  @BindView(R.id.name_layout)
  TextInputLayout nameLayout;

  @BindView(R.id.recycler_view)
  RecyclerView recyclerView;

  @BindView(R.id.fab)
  ExtendedFloatingActionButton fab;

  private CustomFilter filter;
  private CustomFilterAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    filter = getIntent().getParcelableExtra(TOKEN_FILTER);

    super.onCreate(savedInstanceState);

    if (savedInstanceState == null && filter != null) {
      selectedColor = filter.tint;
      selectedIcon = filter.icon;
      name.setText(filter.listingTitle);
    }

    if (savedInstanceState != null) {
      criteria =
          CriterionInstance.fromString(
              filterCriteriaProvider, savedInstanceState.getString(EXTRA_CRITERIA));
    } else if (filter != null) {
      criteria = CriterionInstance.fromString(filterCriteriaProvider, filter.getCriterion());
    } else if (getIntent().hasExtra(EXTRA_CRITERIA)) {
      name.setText(getIntent().getStringExtra(EXTRA_TITLE));
      criteria =
          CriterionInstance.fromString(
              filterCriteriaProvider, getIntent().getStringExtra(EXTRA_CRITERIA));
    } else {
      CriterionInstance instance = new CriterionInstance();
      instance.criterion = filterCriteriaProvider.getStartingUniverse();
      instance.type = CriterionInstance.TYPE_UNIVERSE;
      criteria = newArrayList(instance);
    }

    adapter = new CustomFilterAdapter(criteria, locale, this::onClick);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(adapter);
    new ItemTouchHelper(
            new CustomFilterItemTouchHelper(this, this::onMove, this::onDelete, this::updateList))
        .attachToRecyclerView(recyclerView);

    fab.setExtended(isNew() || adapter.getItemCount() <= 1);

    if (isNew()) {
      toolbar.inflateMenu(R.menu.menu_help);
    }

    updateList();

    updateTheme();
  }

  private void onDelete(int index) {
    criteria.remove(index);
    updateList();
  }

  private void onMove(int from, int to) {
    CriterionInstance criterion = criteria.remove(from);
    criteria.add(to, criterion);
    adapter.notifyItemMoved(from, to);
  }

  private void onClick(String replaceId) {
    CriterionInstance criterionInstance = find(criteria, c -> c.getId().equals(replaceId));

    View view =
        getLayoutInflater().inflate(R.layout.dialog_custom_filter_row_edit, recyclerView, false);
    MaterialButtonToggleGroup group = view.findViewById(R.id.button_toggle);
    int selected = getSelected(criterionInstance);
    group.check(selected);
    dialogBuilder
        .newDialog(criterionInstance.getTitleFromCriterion())
        .setView(view)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              criterionInstance.type = getType(group.getCheckedButtonId());
              updateList();
            })
        .setNeutralButton(R.string.help,(v, which) -> help())
        .show();
  }

  private int getSelected(CriterionInstance instance) {
    switch (instance.type) {
      case CriterionInstance.TYPE_ADD:
        return R.id.button_or;
      case CriterionInstance.TYPE_SUBTRACT:
        return R.id.button_not;
      default:
        return R.id.button_and;
    }
  }

  private int getType(int selected) {
    switch (selected) {
      case R.id.button_or:
        return CriterionInstance.TYPE_ADD;
      case R.id.button_not:
        return CriterionInstance.TYPE_SUBTRACT;
      default:
        return CriterionInstance.TYPE_INTERSECT;
    }
  }

  @OnClick(R.id.fab)
  void addCriteria() {
    AndroidUtilities.hideKeyboard(this);
    fab.shrink();

    List<CustomFilterCriterion> all = filterCriteriaProvider.getAll();
    List<String> names = transform(all, CustomFilterCriterion::getName);
    dialogBuilder.newDialog()
        .setItems(names, (dialog, which) -> {
          CriterionInstance instance = new CriterionInstance();
          instance.criterion = all.get(which);
          showOptionsFor(instance, () -> {
            criteria.add(instance);
            updateList();
          });
          dialog.dismiss();
        })
        .show();
  }

  /** Show options menu for the given criterioninstance */
  private void showOptionsFor(final CriterionInstance item, final Runnable onComplete) {
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
      FrameLayout frameLayout = new FrameLayout(this);
      frameLayout.setPadding(10, 0, 10, 0);
      final EditText editText = new EditText(this);
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

    dialog.show();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(EXTRA_CRITERIA, CriterionInstance.serialize(criteria));
  }

  @Override
  protected boolean isNew() {
    return filter == null;
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

    if (isNullOrEmpty(newName)) {
      nameLayout.setError(getString(R.string.name_cannot_be_empty));
      return;
    }

    if (hasChanges()) {
      org.tasks.data.Filter f = new org.tasks.data.Filter();
      f.setTitle(newName);
      f.setColor(selectedColor);
      f.setIcon(selectedIcon);
      f.setValues(mapToSerializedString(getValues()));
      f.setCriterion(CriterionInstance.serialize(criteria));
      f.setSql(getSql());
      if (isNew()) {
        f.setId(filterDao.insert(f));
      } else {
        f.setId(filter.getId());
        filterDao.update(f);
      }
      setResult(
          RESULT_OK,
          new Intent(TaskListFragment.ACTION_RELOAD)
              .putExtra(MainActivity.OPEN_FILTER, new CustomFilter(f)));
    }

    finish();
  }

  private String getNewName() {
    return name.getText().toString().trim();
  }

  @Override
  protected boolean hasChanges() {
    if (isNew()) {
      return !isNullOrEmpty(getNewName())
          || selectedColor != 0
          || selectedIcon != -1
          || criteria.size() > 1;
    }
    return !getNewName().equals(filter.listingTitle)
        || selectedColor != filter.tint
        || selectedIcon != filter.icon
        || !CriterionInstance.serialize(criteria).equals(filter.getCriterion())
        || !getValues().equals(filter.valuesForNewTasks)
        || !getSql().equals(filter.getOriginalSqlQuery());
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
        RESULT_OK, new Intent(TaskListFragment.ACTION_DELETED).putExtra(TOKEN_FILTER, filter));
    finish();
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.menu_help) {
      help();
      return true;
    } else {
      return super.onMenuItemClick(item);
    }
  }

  private void help() {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://tasks.org/filters")));
  }

  private void updateList() {
    int max = 0, last = -1;

    StringBuilder sql =
        new StringBuilder(Query.select(new CountProperty()).from(Task.TABLE).toString())
            .append(" WHERE ");

    for (CriterionInstance instance : criteria) {
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

      try (Cursor cursor = database.query(sql.toString(), null)) {
        cursor.moveToNext();
        instance.start = last == -1 ? cursor.getInt(0) : last;
        instance.end = cursor.getInt(0);
        last = instance.end;
        max = Math.max(max, last);
      }
    }

    for (CriterionInstance instance : criteria) {
      instance.max = max;
    }

    adapter.submitList(criteria);
  }

  private String getValue(CriterionInstance instance) {
    String value = instance.getValueFromCriterion();
    if (value == null && instance.criterion.sql != null && instance.criterion.sql.contains("?")) {
      value = "";
    }
    return value;
  }

  private String getSql() {
    StringBuilder sql = new StringBuilder(" WHERE ");
    for (CriterionInstance instance : criteria) {
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

  private Map<String, Object> getValues() {
    Map<String, Object> values = new HashMap<>();
    for (CriterionInstance instance : criteria) {
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
}
