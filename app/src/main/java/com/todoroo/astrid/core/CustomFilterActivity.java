/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import static android.text.TextUtils.isEmpty;
import static com.todoroo.andlib.utility.AndroidUtilities.mapToSerializedString;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;

import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.dao.Database;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.FilterDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.filters.FilterCriteriaProvider;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.locale.Locale;
import org.tasks.preferences.Preferences;
import org.tasks.ui.MenuColorizer;

/**
 * Activity that allows users to build custom filters
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class CustomFilterActivity extends ThemedInjectingAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  public static final String TOKEN_FILTER = "token_filter";
  public static final String ACTION_FILTER_RENAMED = "filterRenamed";
  public static final String ACTION_FILTER_DELETED = "filterDeleted";

  static final int MENU_GROUP_CONTEXT_TYPE = 1;
  static final int MENU_GROUP_CONTEXT_DELETE = 2;
  private static final int MENU_GROUP_FILTER = 0;

  // --- hierarchy of filter classes
  @Inject
  Database database;
  @Inject
  protected FilterDao filterDao;
  @Inject
  protected DialogBuilder dialogBuilder;
  @Inject
  Preferences preferences;

  // --- activity
  @Inject
  FilterCriteriaProvider filterCriteriaProvider;
  @Inject
  Locale locale;

  @BindView(R.id.tag_name)
  protected EditText filterName;

  @BindView(R.id.toolbar)
  protected Toolbar toolbar;

  private ListView listView;
  private CustomFilterAdapter adapter;
  protected CustomFilter filter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.custom_filter_activity);
    ButterKnife.bind(this);

    final boolean backButtonSavesTask = preferences.backButtonSavesTask();
    toolbar.setNavigationIcon(
            ContextCompat.getDrawable(
                    this, backButtonSavesTask ? R.drawable.ic_outline_clear_24px : R.drawable.ic_outline_save_24px));
    toolbar.setNavigationOnClickListener(
            v -> {
              if (backButtonSavesTask) {
                discard();
              } else {
                save();
              }
            });

    toolbar.setOnMenuItemClickListener(this);
    database.openForReading();
    filter = getIntent().getParcelableExtra(TOKEN_FILTER);
    if (filter==null) {
      toolbar.setTitle(R.string.FLA_new_filter);
      adapter = new CustomFilterAdapter(this, database, filterCriteriaProvider, dialogBuilder, locale);
    } else {
      toolbar.setTitle(filter.listingTitle);
      toolbar.inflateMenu(R.menu.menu_tag_settings);
      filterName.setText(filter.listingTitle);
      adapter = new CustomFilterAdapter(this, database, filterCriteriaProvider, filter.getCriterion(), dialogBuilder, locale);
    }
    MenuColorizer.colorToolbar(this, toolbar);
    listView = findViewById(android.R.id.list);
    listView.setAdapter(adapter);
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
    imm.hideSoftInputFromWindow(filterName.getWindowToken(), 0);
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

  @OnTextChanged(R.id.tag_name)
  void onTextChanged(CharSequence ignored) {
    filterName.setError(null);
  }


  protected void save() {
    String title = filterName.getText().toString().trim();
    if (isEmpty(title)) {
      filterName.setError(getString(R.string.name_cannot_be_empty));
      return;
    }
    boolean nameChanged = filter!=null && !filter.listingTitle.equals(filterName.getText().toString().trim());

    Map<String, Object> values = new HashMap<>();
    String sql = adapter.toSql(values);

    org.tasks.data.Filter storeObject = persist(filter!=null?filter.getId():null, title, sql, values);
    filter = new CustomFilter(title, sql, values, storeObject.getId(), storeObject.getCriterion());

    if (nameChanged) {
      setResult(RESULT_OK, new Intent(CustomFilterActivity.ACTION_FILTER_RENAMED).putExtra(CustomFilterActivity.TOKEN_FILTER, filter));
    } else {
      setResult(RESULT_OK, new Intent().putExtra(MainActivity.OPEN_FILTER, filter));
    }

    finish();
  }



  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.delete:
        if (filter==null) {
          discard();
        } else {
          delete();
        }
        break;
    }
    return onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {
    if (preferences.backButtonSavesTask()) {
      save();
    } else {
      discard();
    }
  }

  protected boolean isUnchanged() {
    boolean result = true;
    if (filter!=null) {
      result = filter.listingTitle.equals(this.filterName.getText().toString().trim());
    }
    return result && filterName.getText().toString().trim().isEmpty() && adapter.getCount() <= 1;
  }

  protected void delete() {
      dialogBuilder
              .newMessageDialog(R.string.delete_tag_confirmation, filter.listingTitle)
              .setPositiveButton(
                      R.string.delete,
                      (dialog, which) -> {
                        filterDao.delete(filter.getId());
                        setResult(
                                RESULT_OK, new Intent(ACTION_FILTER_DELETED).putExtra(TOKEN_FILTER, filter));
                        finish();
                      })
              .setNegativeButton(android.R.string.cancel, null)
              .show();

  }

  private void discard() {
    if (isUnchanged()) {
      finish();
    } else {
      dialogBuilder
          .newMessageDialog(R.string.discard_changes)
          .setPositiveButton(R.string.keep_editing, null)
          .setNegativeButton(R.string.discard, (dialog, which) -> finish())
          .show();
    }
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    if (item.getGroupId() == MENU_GROUP_FILTER) {
      // give an initial value for the row before adding it
      adapter.addItem(item.getItemId());
      return true;
    }

    // item type context item
    else if (item.getGroupId() == MENU_GROUP_CONTEXT_TYPE) {
      adapter.setItemType(item.getOrder(), item.getItemId());
    }

    // delete context item
    else if (item.getGroupId() == MENU_GROUP_CONTEXT_DELETE) {
      adapter.removeItem(item.getOrder());
    }

    return super.onContextItemSelected(item);
  }

  private org.tasks.data.Filter persist(Long filterId, String title, String sql, Map<String, Object> values) {
    // if filter of this name exists, edit it
    final org.tasks.data.Filter filter;
    if (filterId == null) {
      filter = new org.tasks.data.Filter();
    } else {
      filter = filterDao.getById(filterId);
    }

    // populate saved filter properties
    filter.setTitle(title);
    filter.setSql(sql);
    filter.setValues(values == null ? "" : mapToSerializedString(values));
    filter.setCriterion(adapter.serializeFilters());

    filter.setId(filterDao.insertOrUpdate(filter));
    return filter.getId() >= 0 ? filter : null;
  }

  public static class CriterionInstance {

    public static final int TYPE_ADD = 0;
    public static final int TYPE_SUBTRACT = 1;
    public static final int TYPE_INTERSECT = 2;
    public static final int TYPE_UNIVERSE = 3;

    /** criteria for this instance */
    public CustomFilterCriterion criterion;

    /** which of the entries is selected (MultipleSelect) */
    public int selectedIndex = -1;

    /** text of selection (TextInput) */
    public String selectedText = null;

    /** type of join */
    public int type = TYPE_INTERSECT;

    /** statistics for filter count */
    public int end;
    public int start;
    public int max;
  }

}
