/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package org.tasks.activities;

import static android.text.TextUtils.isEmpty;
import static com.google.common.collect.Lists.transform;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.FilterDao;
import org.tasks.dialogs.AlertDialogBuilder;
import org.tasks.filters.FilterCriteriaProvider;
import org.tasks.injection.ActivityComponent;
import org.tasks.locale.Locale;

public class FilterSettingsActivity extends BaseListSettingsActivity {

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

  @BindView(R.id.recycler_view)
  RecyclerView recyclerView;

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
    adapter = new CustomFilterAdapter(criteria, locale, this::onClick);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(adapter);

    fab.setExtended(adapter.getItemCount() <= 1);

    updateList();

    updateTheme();
  }

  private void onClick(CriterionInstance criterionInstance) {
    View view =
        getLayoutInflater().inflate(R.layout.dialog_custom_filter_row_edit, recyclerView, false);
    MaterialButtonToggleGroup group = view.findViewById(R.id.button_toggle);
    int selected = getSelected(criterionInstance);
    group.check(selected);
    AlertDialog d = dialogBuilder
        .newDialog(criterionInstance.getTitleFromCriterion())
        .setView(view)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              criterionInstance.type = getType(group.getCheckedButtonId());
              updateList();
            })
        .show();
    view.findViewById(R.id.delete).setOnClickListener(v -> {
      d.dismiss();
      adapter.remove(criterionInstance);
      updateList();
    });
    view.findViewById(R.id.reconfigure).setOnClickListener(v -> {
      d.dismiss();
      addCriteria(criterionInstance);
    });
  }

  private int getSelected(CriterionInstance instance) {
    switch (instance.type) {
      case CriterionInstance.TYPE_ADD:
        return R.id.button_or;
      case CriterionInstance.TYPE_INTERSECT:
        return R.id.button_and;
      case CriterionInstance.TYPE_SUBTRACT:
        return R.id.button_not;
      default:
        throw new RuntimeException();
    }
  }

  private int getType(int selected) {
    switch (selected) {
      case R.id.button_and:
        return CriterionInstance.TYPE_INTERSECT;
      case R.id.button_or:
        return CriterionInstance.TYPE_ADD;
      case R.id.button_not:
        return CriterionInstance.TYPE_SUBTRACT;
      default:
        throw new RuntimeException();
    }
  }

  @OnClick(R.id.fab)
  void addCriteria() {
    addCriteria(null);
    fab.shrink();
  }

  private void addCriteria(@Nullable CriterionInstance replace) {
    AndroidUtilities.hideKeyboard(this);

    List<CustomFilterCriterion> all = filterCriteriaProvider.getAll();
    List<String> names = transform(all, CustomFilterCriterion::getName);
    dialogBuilder.newDialog()
        .setItems(names, (dialog, which) -> {
          CriterionInstance instance = new CriterionInstance();
          instance.criterion = all.get(which);
          showOptionsFor(instance, () -> {
            if (replace == null) {
              adapter.add(instance);
            } else {
              adapter.replace(replace, instance);
            }
            updateList();
          });
          dialog.dismiss();
        })
        .show();
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
      filter.setId(filterDao.insertOrUpdate(filter.toStoreObject()));
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
        RESULT_OK, new Intent(TaskListFragment.ACTION_DELETED).putExtra(TOKEN_FILTER, filter));
    finish();
  }

  public void updateList() {
    int max = 0, last = -1;

    StringBuilder sql =
        new StringBuilder(Query.select(new CountProperty()).from(Task.TABLE).toString())
            .append(" WHERE ");

    for (CriterionInstance instance : adapter.getItems()) {
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

    for (CriterionInstance instance : adapter.getItems()) {
      instance.max = max;
    }

    adapter.notifyDataSetChanged();
  }
}
