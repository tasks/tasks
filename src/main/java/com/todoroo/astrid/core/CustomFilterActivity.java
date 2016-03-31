/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.UnaryCriterion;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.api.TextInputCriterion;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.filters.FilterCriteriaProvider;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.ui.MenuColorizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

import static android.text.TextUtils.isEmpty;

/**
 * Activity that allows users to build custom filters
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class CustomFilterActivity extends ThemedInjectingAppCompatActivity {

    private static final String IDENTIFIER_UNIVERSE = "active"; //$NON-NLS-1$

    static final int MENU_GROUP_FILTER = 0;
    static final int MENU_GROUP_CONTEXT_TYPE = 1;
    static final int MENU_GROUP_CONTEXT_DELETE = 2;

    // --- hierarchy of filter classes

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
        public int start, end, max;

        public String getTitleFromCriterion() {
            if(criterion instanceof MultipleSelectCriterion) {
                if(selectedIndex >= 0 && ((MultipleSelectCriterion)criterion).entryTitles != null &&
                        selectedIndex < ((MultipleSelectCriterion)criterion).entryTitles.length) {
                    String title = ((MultipleSelectCriterion)criterion).entryTitles[selectedIndex];
                    return criterion.text.replace("?", title);
                }
                return criterion.text;
            } else if(criterion instanceof TextInputCriterion) {
                if(selectedText == null) {
                    return criterion.text;
                }
                return criterion.text.replace("?", selectedText);
            }
            throw new UnsupportedOperationException("Unknown criterion type"); //$NON-NLS-1$
        }

        public String getValueFromCriterion() {
            if(type == TYPE_UNIVERSE) {
                return null;
            }
            if(criterion instanceof MultipleSelectCriterion) {
                if(selectedIndex >= 0 && ((MultipleSelectCriterion)criterion).entryValues != null &&
                        selectedIndex < ((MultipleSelectCriterion)criterion).entryValues.length) {
                    return ((MultipleSelectCriterion)criterion).entryValues[selectedIndex];
                }
                return criterion.text;
            } else if(criterion instanceof TextInputCriterion) {
                return selectedText;
            }
            throw new UnsupportedOperationException("Unknown criterion type"); //$NON-NLS-1$
        }
    }

    private ListView listView;

    private CustomFilterAdapter adapter;

    // --- activity

    @Inject Database database;
    @Inject StoreObjectDao storeObjectDao;
    @Inject DialogBuilder dialogBuilder;
    @Inject FilterCriteriaProvider filterCriteriaProvider;

    @Bind(R.id.tag_name) EditText filterName;
    @Bind(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.custom_filter_activity);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_close_24dp));
            DrawableCompat.setTint(drawable, getResources().getColor(android.R.color.white));
            supportActionBar.setHomeAsUpIndicator(drawable);
            supportActionBar.setTitle(R.string.FLA_new_filter);
        }

        listView = (ListView) findViewById(android.R.id.list);

        database.openForReading();

        List<CriterionInstance> startingCriteria = new ArrayList<>();
        startingCriteria.add(getStartingUniverse());
        adapter = new CustomFilterAdapter(this, dialogBuilder, startingCriteria);
        listView.setAdapter(adapter);
        updateList();

        setUpListeners();
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    private CriterionInstance getStartingUniverse() {
        CriterionInstance instance = new CriterionInstance();
        instance.criterion = new MultipleSelectCriterion(IDENTIFIER_UNIVERSE,
                getString(R.string.CFA_universe_all),
                null, null, null, null, null, null);
        instance.type = CriterionInstance.TYPE_UNIVERSE;
        return instance;
    }

    private void setUpListeners() {
        findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.showContextMenu();
            }
        });

        listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                if(menu.hasVisibleItems()) {
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
            }
        });
    }

    @Override
    public void finish() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(filterName.getWindowToken(), 0);
        super.finish();
    }


    // --- listeners and action events

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if(menu.size() > 0) {
            menu.clear();
        }

        // view holder
        if(v.getTag() != null) {
            adapter.onCreateContextMenu(menu, v);
        }
    }

    void saveAndView() {
        String title = filterName.getText().toString().trim();

        if (isEmpty(title)) {
            return;
        }

        StringBuilder sql = new StringBuilder(" WHERE ");
        ContentValues values = new ContentValues();
        for(int i = 0; i < adapter.getCount(); i++) {
            CriterionInstance instance = adapter.getItem(i);
            String value = instance.getValueFromCriterion();
            if(value == null && instance.criterion.sql != null && instance.criterion.sql.contains("?")) {
                value = "";
            }

            switch(instance.type) {
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
            if(instance.criterion.sql == null) {
                sql.append(TaskCriteria.activeAndVisible()).append(' ');
            } else {
                String subSql = instance.criterion.sql.replace("?", UnaryCriterion.sanitize(value));
                sql.append(Task.ID).append(" IN (").append(subSql).append(") ");
            }

            if(instance.criterion.valuesForNewTasks != null &&
                    instance.type == CriterionInstance.TYPE_INTERSECT) {
                for(Entry<String, Object> entry : instance.criterion.valuesForNewTasks.valueSet()) {
                    values.put(entry.getKey().replace("?", value),
                            entry.getValue().toString().replace("?", value));
                }
            }
        }

        StoreObject storeObject = SavedFilter.persist(storeObjectDao, adapter, title, sql.toString(), values);
        Filter filter = new CustomFilter(title, sql.toString(), values, storeObject.getId());
        setResult(RESULT_OK, new Intent().putExtra(TagSettingsActivity.TOKEN_NEW_FILTER, filter));
        finish();
    }

    /**
     * Recalculate all sizes
     */
    void updateList() {
        int max = 0, last = -1;

        StringBuilder sql = new StringBuilder(Query.select(new CountProperty()).from(Task.TABLE).toString()).
            append(" WHERE ");

        for(int i = 0; i < adapter.getCount(); i++) {
            CriterionInstance instance = adapter.getItem(i);
            String value = instance.getValueFromCriterion();
            if(value == null && instance.criterion.sql != null && instance.criterion.sql.contains("?")) {
                value = "";
            }

            switch(instance.type) {
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
            if(instance.criterion.sql == null) {
                sql.append(TaskCriteria.activeAndVisible()).append(' ');
            } else {
                String subSql = instance.criterion.sql.replace("?", UnaryCriterion.sanitize(value));
                subSql = PermaSql.replacePlaceholders(subSql);
                sql.append(Task.ID).append(" IN (").append(subSql).append(") ");
            }

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

        for(int i = 0; i < adapter.getCount(); i++) {
            CriterionInstance instance = adapter.getItem(i);
            instance.max = max;
        }

        adapter.notifyDataSetInvalidated();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tag_settings_activity, menu);
        MenuColorizer.colorMenu(this, menu, getResources().getColor(android.R.color.white));
        menu.findItem(R.id.delete).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                discard();
                break;
            case R.id.menu_save:
                saveAndView();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        discard();
    }

    private void discard() {
        if (filterName.getText().toString().trim().isEmpty() && adapter.getCount() <= 1) {
            finish();
        } else {
            dialogBuilder.newMessageDialog(R.string.discard_changes)
                    .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if(item.getGroupId() == MENU_GROUP_FILTER) {
            // give an initial value for the row before adding it
            CustomFilterCriterion criterion = filterCriteriaProvider.getAll().get(item.getItemId());
            final CriterionInstance instance = new CriterionInstance();
            instance.criterion = criterion;
            adapter.showOptionsFor(instance, new Runnable() {
                @Override
                public void run() {
                    adapter.add(instance);
                    updateList();
                }
            });
            return true;
        }

        // item type context item
        else if(item.getGroupId() == MENU_GROUP_CONTEXT_TYPE) {
            CriterionInstance instance = adapter.getItem(item.getOrder());
            instance.type = item.getItemId();
            updateList();
        }

        // delete context item
        else if(item.getGroupId() == MENU_GROUP_CONTEXT_DELETE) {
            CriterionInstance instance = adapter.getItem(item.getOrder());
            adapter.remove(instance);
            updateList();
        }

        return super.onContextItemSelected(item);
    }
}
