package com.todoroo.astrid.core;

import java.util.*;
import java.util.Map.Entry;

import android.app.ListActivity;
import android.content.*;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.Button;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.*;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;

/**
 * Activity that allows users to build custom filters
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class CustomFilterActivity extends ListActivity {

    private static final String IDENTIFIER_TITLE = "title"; //$NON-NLS-1$
    private static final String IDENTIFIER_IMPORTANCE = "importance"; //$NON-NLS-1$
    private static final String IDENTIFIER_DUEDATE = "dueDate"; //$NON-NLS-1$
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

        @SuppressWarnings("nls")
        public String getTitleFromCriterion() {
            if(criterion instanceof MultipleSelectCriterion) {
                if(selectedIndex >= 0 && ((MultipleSelectCriterion)criterion).entryTitles != null &&
                        selectedIndex < ((MultipleSelectCriterion)criterion).entryTitles.length) {
                    String title = ((MultipleSelectCriterion)criterion).entryTitles[selectedIndex];
                    return criterion.text.replace("?", title);
                }
                return criterion.text;
            } else if(criterion instanceof TextInputCriterion) {
                if(selectedText == null)
                    return criterion.text;
                return criterion.text.replace("?", selectedText);
            }
            throw new UnsupportedOperationException("Unknown criterion type"); //$NON-NLS-1$
        }

        public String getValueFromCriterion() {
            if(type == TYPE_UNIVERSE)
                return null;
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

    private TextView filterName;
    private CustomFilterAdapter adapter;
    private final Map<String,CustomFilterCriterion> criteria = Collections.synchronizedMap(new LinkedHashMap<String,CustomFilterCriterion>());

    private FilterCriteriaReceiver filterCriteriaReceiver = new FilterCriteriaReceiver();

    // --- activity

    @Autowired
    Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);

        setContentView(R.layout.custom_filter_activity);
        setTitle(R.string.CFA_title);

        DependencyInjectionService.getInstance().inject(this);
        database.openForReading();
        populateCriteria();

        filterName = (TextView)findViewById(R.id.filterName);
        List<CriterionInstance> startingCriteria = new ArrayList<CriterionInstance>();
        startingCriteria.add(getStartingUniverse());
        adapter = new CustomFilterAdapter(this, startingCriteria);
        setListAdapter(adapter);
        updateList();

        setUpListeners();
    }

    /**
     * Populate criteria list with built in and plugin criteria. The request is sent to every application
     * registered to listen for this broadcast. Each plugin can then add criteria to this activity.
     */
    @SuppressWarnings("nls")
    private void populateCriteria() {
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_CUSTOM_FILTER_CRITERIA);
        sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

        Resources r = getResources();

        // built in criteria: due date
        {
            String[] entryValues = new String[] {
                    "0",
                    PermaSql.VALUE_EOD_YESTERDAY,
                    PermaSql.VALUE_EOD,
                    PermaSql.VALUE_EOD_TOMORROW,
                    PermaSql.VALUE_EOD_DAY_AFTER,
                    PermaSql.VALUE_EOD_NEXT_WEEK,
                    PermaSql.VALUE_EOD_NEXT_MONTH,
            };
            ContentValues values = new ContentValues();
            values.put(Task.DUE_DATE.name, "?");
            CustomFilterCriterion criterion = new MultipleSelectCriterion(
                    IDENTIFIER_DUEDATE,
                    getString(R.string.CFC_dueBefore_text),
                    Query.select(Task.ID).from(Task.TABLE).where(
                            Criterion.and(
                                    TaskCriteria.activeAndVisible(),
                                    Criterion.or(
                                            Field.field("?").eq(0),
                                            Task.DUE_DATE.gt(0)),
                                    Task.DUE_DATE.lte("?"))).toString(),
                    values, r.getStringArray(R.array.CFC_dueBefore_entries),
                    entryValues, ((BitmapDrawable)r.getDrawable(R.drawable.tango_calendar)).getBitmap(),
                    getString(R.string.CFC_dueBefore_name));
            criteria.put(IDENTIFIER_DUEDATE, criterion);
        }

        // built in criteria: importance
        {
            String[] entryValues = new String[] {
                            Integer.toString(Task.IMPORTANCE_DO_OR_DIE),
                            Integer.toString(Task.IMPORTANCE_MUST_DO),
                            Integer.toString(Task.IMPORTANCE_SHOULD_DO),
                            Integer.toString(Task.IMPORTANCE_NONE),
                    };
            String[] entries = new String[] {
                    "!!!!", "!!!", "!!", "!"
            };
            ContentValues values = new ContentValues();
            values.put(Task.IMPORTANCE.name, "?");
            CustomFilterCriterion criterion = new MultipleSelectCriterion(
                    IDENTIFIER_IMPORTANCE,
                    getString(R.string.CFC_importance_text),
                    Query.select(Task.ID).from(Task.TABLE).where(
                            Criterion.and(TaskCriteria.activeAndVisible(),
                                    Task.IMPORTANCE.lte("?"))).toString(),
                    values, entries,
                    entryValues, ((BitmapDrawable)r.getDrawable(R.drawable.tango_warning)).getBitmap(),
                    getString(R.string.CFC_importance_name));
            criteria.put(IDENTIFIER_IMPORTANCE, criterion);
        }

        // built in criteria: title containing X
        {
            ContentValues values = new ContentValues();
            values.put(Task.TITLE.name, "?");
            CustomFilterCriterion criterion = new TextInputCriterion(
                    IDENTIFIER_TITLE,
                    getString(R.string.CFC_title_contains_text),
                    Query.select(Task.ID).from(Task.TABLE).where(
                            Criterion.and(TaskCriteria.activeAndVisible(),
                                    Task.TITLE.like("%?%"))).toString(),
                        null, getString(R.string.CFC_title_contains_name), "",
                        ((BitmapDrawable)r.getDrawable(R.drawable.tango_alpha)).getBitmap(),
                        getString(R.string.CFC_title_contains_name));
            criteria.put(IDENTIFIER_TITLE, criterion);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(filterCriteriaReceiver, new IntentFilter(AstridApiConstants.BROADCAST_SEND_CUSTOM_FILTER_CRITERIA));
        populateCriteria();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(filterCriteriaReceiver);
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
        ((Button)findViewById(R.id.add)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListView().showContextMenu();
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
                    saveAndView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.tango_next, 0);
                } else {
                    saveAndView.setText(R.string.CFA_button_save);
                    saveAndView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.tango_save, 0);
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

        getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
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
                for (CustomFilterCriterion item : criteria.values()) {
                    try {
                        menu.add(CustomFilterActivity.MENU_GROUP_FILTER,
                                i, 0, item.name);
                    } catch (NullPointerException e) {
                        throw new NullPointerException("One of the criteria is null. Criteria: " + criteria);
                    }
                    i++;
                }
            }
        });
    }

    // --- listeners and action events

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if(menu.size() > 0)
            menu.clear();

        // view holder
        if(v.getTag() != null) {
            adapter.onCreateContextMenu(menu, v);
        }
    }

    @SuppressWarnings("nls")
    void saveAndView() {
        StringBuilder sql = new StringBuilder(" WHERE ");
        StringBuilder suggestedTitle = new StringBuilder();
        ContentValues values = new ContentValues();
        for(int i = 0; i < adapter.getCount(); i++) {
            CriterionInstance instance = adapter.getItem(i);
            String value = instance.getValueFromCriterion();
            if(value == null && instance.criterion.sql != null && instance.criterion.sql.contains("?"))
                value = "";

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
            if(instance.criterion.sql == null)
                sql.append(TaskCriteria.activeAndVisible()).append(' ');
            else {
                String subSql = instance.criterion.sql.replace("?", value);
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
        if(filterName.getText().length() > 0) {
            // persist saved filter
            title = filterName.getText().toString().trim();
            SavedFilter.persist(adapter, title, sql.toString(), values);
        } else {
            // temporary
            title = suggestedTitle.toString();
        }

        // view
        Filter filter = new Filter(title, title, sql.toString(), values);
        Intent taskListActivity = new Intent(this, TaskListActivity.class);
        taskListActivity.putExtra(TaskListActivity.TOKEN_FILTER, filter);
        startActivity(taskListActivity);
    }

    /**
     * Recalculate all sizes
     */
    @SuppressWarnings("nls")
    void updateList() {
        int max = 0, last = -1;

        StringBuilder sql = new StringBuilder(Query.select(new CountProperty()).from(Task.TABLE).toString()).
            append(" WHERE ");

        for(int i = 0; i < adapter.getCount(); i++) {
            CriterionInstance instance = adapter.getItem(i);
            String value = instance.getValueFromCriterion();
            if(value == null && instance.criterion.sql != null && instance.criterion.sql.contains("?"))
                value = "";

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
            if(instance.criterion.sql == null)
                sql.append(TaskCriteria.activeAndVisible()).append(' ');
            else {
                String subSql = instance.criterion.sql.replace("?", value);
                subSql = PermaSql.replacePlaceholders(subSql);
                sql.append(Task.ID).append(" IN (").append(subSql).append(") ");
            }

            Cursor cursor = database.getDatabase().rawQuery(sql.toString(), null);
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

    private <V> V getNth(int index, Map<?,V> map) {
        int i = 0;
        for (V v : map.values()) {
            if (i == index) return v;
            i++;
        }
        throw new IllegalArgumentException("out of bounds");
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // group filter option
        if(item.getGroupId() == MENU_GROUP_FILTER) {
            // give an initial value for the row before adding it
            CustomFilterCriterion criterion = getNth(item.getItemId(), criteria);
            final CriterionInstance instance = new CriterionInstance();
            instance.criterion = criterion;
            adapter.showOptionsFor(instance, new Runnable() {
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

        return super.onMenuItemSelected(featureId, item);
    }

    public class FilterCriteriaReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final Parcelable[] filters = intent.getExtras().
                    getParcelableArray(AstridApiConstants.EXTRAS_RESPONSE);
                for (Parcelable filter : filters) {
                    CustomFilterCriterion filterCriterion = (CustomFilterCriterion) filter;
                    criteria.put(filterCriterion.identifier, filterCriterion);
                }
            } catch (Exception e) {
                String addon;
                try {
                    addon = intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON);
                } catch (Exception e1) {
                    Log.e("receive-custom-filter-criteria-error-retrieving-addon",
                            e.toString(), e);
                    return;
                }
                Log.e("receive-custom-filter-criteria-" +  //$NON-NLS-1$
                        addon,
                        e.toString(), e);
            }
        }
    }
}
