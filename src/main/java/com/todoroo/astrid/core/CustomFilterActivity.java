/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.UnaryCriterion;
import com.todoroo.andlib.utility.AndroidUtilities;
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
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ActivityPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;

/**
 * Activity that allows users to build custom filters
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class CustomFilterActivity extends InjectingAppCompatActivity {

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

        /** statistics for {@link FilterView} */
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
    private TextView filterName;

    private CustomFilterAdapter adapter;

    // --- activity

    @Inject Database database;
    @Inject StoreObjectDao storeObjectDao;
    @Inject ActivityPreferences preferences;
    @Inject DialogBuilder dialogBuilder;
    @Inject FilterCriteriaProvider filterCriteriaProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences.applyTheme();

        setContentView(R.layout.custom_filter_activity);
        setTitle(R.string.FLA_new_filter);

        listView = (ListView) findViewById(android.R.id.list);

        database.openForReading();

        filterName = (TextView)findViewById(R.id.filterName);
        List<CriterionInstance> startingCriteria = new ArrayList<>();
        startingCriteria.add(getStartingUniverse());
        adapter = new CustomFilterAdapter(this, dialogBuilder, startingCriteria);
        listView.setAdapter(adapter);
        updateList();

        setUpListeners();
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

        final Button saveAndView = ((Button)findViewById(R.id.saveAndView));
        saveAndView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndView();
            }
        });

        filterName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() == 0) {
                    saveAndView.setText(R.string.CFA_button_view);
                } else {
                    saveAndView.setText(R.string.CFA_button_save);
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                //
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                //
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
        super.finish();
        if (!preferences.useTabletLayout()) {
            AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
        }
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
        StringBuilder sql = new StringBuilder(" WHERE ");
        StringBuilder suggestedTitle = new StringBuilder();
        ContentValues values = new ContentValues();
        for(int i = 0; i < adapter.getCount(); i++) {
            CriterionInstance instance = adapter.getItem(i);
            String value = instance.getValueFromCriterion();
            if(value == null && instance.criterion.sql != null && instance.criterion.sql.contains("?")) {
                value = "";
            }

            String title = instance.getTitleFromCriterion();

            switch(instance.type) {
            case CriterionInstance.TYPE_ADD:
                sql.append("OR ");
                suggestedTitle.append(getString(R.string.CFA_type_add)).append(' ').
                    append(title).append(' ');
                break;
            case CriterionInstance.TYPE_SUBTRACT:
                sql.append("AND NOT ");
                suggestedTitle.append(getString(R.string.CFA_type_subtract)).append(' ').
                    append(title).append(' ');
                break;
            case CriterionInstance.TYPE_INTERSECT:
                sql.append("AND ");
                suggestedTitle.append(title).append(' ');
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

        String title;
        StoreObject storeObject = null;
        if(filterName.getText().length() > 0) {
            // persist saved filter
            title = filterName.getText().toString().trim();
            storeObject = SavedFilter.persist(storeObjectDao, adapter, title, sql.toString(), values);
        } else {
            // temporary
            title = suggestedTitle.toString();
        }

        // view
        Filter filter = new CustomFilter(title, sql.toString(), values, storeObject == null ? -1L : storeObject.getId());
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
