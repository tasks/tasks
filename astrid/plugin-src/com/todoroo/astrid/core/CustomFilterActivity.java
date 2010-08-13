package com.todoroo.astrid.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
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
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TagService.Tag;

/**
 * Activity that allows users to build custom filters
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class CustomFilterActivity extends ListActivity {

    private static final String IDENTIFIER_TAG = "tag"; //$NON-NLS-1$
    private static final String IDENTIFIER_IMPORTANCE = "importance"; //$NON-NLS-1$
    private static final String IDENTIFIER_DUEDATE = "dueDate"; //$NON-NLS-1$
    private static final String IDENTIFIER_UNIVERSE = "active"; //$NON-NLS-1$

    static final int MENU_GROUP_FILTER = 0;
    static final int MENU_GROUP_FILTER_OPTION = 1;
    static final int MENU_GROUP_CONTEXT_TYPE = 2;
    static final int MENU_GROUP_CONTEXT_DELETE = 3;

    // --- hierarchy of filter classes

    public static class CriterionInstance {
        public static final int TYPE_ADD = 0;
        public static final int TYPE_SUBTRACT = 1;
        public static final int TYPE_INTERSECT = 2;
        public static final int TYPE_UNIVERSE = 3;

        /** criteria for this instance */
        public CustomFilterCriterion criterion;

        /** which of the entries is selected */
        public int selectedIndex = -1;

        /** type of join */
        public int type = TYPE_INTERSECT;

        /** statistics for {@link FilterView} */
        public int start, end, max;
    }

    private TextView filterName;
    private CustomFilterAdapter adapter;
    private final ArrayList<CustomFilterCriterion> criteria =
        new ArrayList<CustomFilterCriterion>();

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
     * Populate criteria list with built in and plugin criteria
     */
    @SuppressWarnings("nls")
    private void populateCriteria() {
        Resources r = getResources();

        // built in criteria: due date
        String[] entryValues = new String[] {
                "0",
                PermaSql.VALUE_EOD_YESTERDAY,
                PermaSql.VALUE_EOD,
                PermaSql.VALUE_EOD_TOMORROW,
                PermaSql.VALUE_EOD_DAY_AFTER,
                PermaSql.VALUE_EOD_NEXT_WEEK,
        };
        ContentValues values = new ContentValues();
        values.put(Task.DUE_DATE.name, "?");
        CustomFilterCriterion criterion = new CustomFilterCriterion(
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
        criteria.add(criterion);

        // built in criteria: importance
        entryValues = new String[] {
                Integer.toString(Task.IMPORTANCE_DO_OR_DIE),
                Integer.toString(Task.IMPORTANCE_MUST_DO),
                Integer.toString(Task.IMPORTANCE_SHOULD_DO),
                Integer.toString(Task.IMPORTANCE_NONE),
        };
        String[] entries = new String[] {
                "!!!!", "!!!", "!!", "!"
        };
        values = new ContentValues();
        values.put(Task.IMPORTANCE.name, "?");
        criterion = new CustomFilterCriterion(
                IDENTIFIER_IMPORTANCE,
                getString(R.string.CFC_importance_text),
                Query.select(Task.ID).from(Task.TABLE).where(
                        Criterion.and(TaskCriteria.activeAndVisible(),
                                Task.IMPORTANCE.lte("?"))).toString(),
                values, entries,
                entryValues, ((BitmapDrawable)r.getDrawable(R.drawable.tango_warning)).getBitmap(),
                getString(R.string.CFC_importance_name));
        criteria.add(criterion);

        // built in criteria: tags
        Tag[] tags = TagService.getInstance().getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE, Criterion.all);
        String[] tagNames = new String[tags.length];
        for(int i = 0; i < tags.length; i++)
            tagNames[i] = tags[i].tag;
        values = new ContentValues();
        values.put(Metadata.KEY.name, TagService.KEY);
        values.put(TagService.TAG.name, "?");
        criterion = new CustomFilterCriterion(
                IDENTIFIER_TAG,
                getString(R.string.CFC_tag_text),
                Query.select(Metadata.TASK).from(Metadata.TABLE).join(Join.inner(
                            Task.TABLE, Metadata.TASK.eq(Task.ID))).where(Criterion.and(
                        TaskCriteria.activeAndVisible(),
                        MetadataCriteria.withKey(TagService.KEY),
                        TagService.TAG.eq("?"))).toString(),
                values, tagNames, tagNames,
                ((BitmapDrawable)r.getDrawable(R.drawable.filter_tags1)).getBitmap(),
                getString(R.string.CFC_tag_name));
        criteria.add(criterion);
    }

    private CriterionInstance getStartingUniverse() {
        CriterionInstance instance = new CriterionInstance();
        instance.criterion = new CustomFilterCriterion(IDENTIFIER_UNIVERSE,
                getString(R.string.CFA_universe_all),
                null, null, null, null, null, null);
        instance.type = CriterionInstance.TYPE_UNIVERSE;
        return instance;
    }

    private void setUpListeners() {
        ((Button)findViewById(R.id.add)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuItemInstance = null;
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
                if(menu.hasVisibleItems())
                    return;

                if(menuItemInstance == null) {
                    for(int i = 0; i < criteria.size(); i++) {
                        CustomFilterCriterion item = criteria.get(i);
                        SubMenu subMenu = menu.addSubMenu(item.name);
                        if(item.icon != null)
                            subMenu.setIcon(new BitmapDrawable(item.icon));

                        for(int j = 0; j < item.entryTitles.length; j++) {
                            subMenu.add(CustomFilterActivity.MENU_GROUP_FILTER_OPTION,
                                    i, j, item.entryTitles[j]);
                        }
                    }
                }

                // was invoked by short-pressing row
                else {
                    CustomFilterCriterion criterion = menuItemInstance.criterion;
                    if(criterion.entryTitles == null ||
                            criterion.entryTitles.length == 0)
                        return;

                    menu.setHeaderTitle(criterion.name);
                    menu.setGroupCheckable(CustomFilterActivity.MENU_GROUP_FILTER_OPTION, true, true);

                    for(int i = 0; i < criterion.entryTitles.length; i++) {
                        menu.add(CustomFilterActivity.MENU_GROUP_FILTER_OPTION,
                                -1, i, criterion.entryTitles[i]);
                    }
                }
            }
        });
    }

    // --- listeners and action events

    CriterionInstance menuItemInstance = null;

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
            if(instance.selectedIndex < 0 && instance.criterion.entryValues != null)
                continue;

            String entryTitle = "";
            if(instance.criterion.entryTitles != null) {
                entryTitle = instance.criterion.entryTitles[instance.selectedIndex];
            }
            String title = instance.criterion.text.replace("?", entryTitle);


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


            String entryValue = "";
            if(instance.criterion.entryValues != null && instance.selectedIndex > -1)
                entryValue = instance.criterion.entryValues[instance.selectedIndex];

            // special code for all tasks universe
            if(instance.criterion.sql == null)
                sql.append(TaskCriteria.activeAndVisible()).append(' ');
            else {
                String subSql = instance.criterion.sql.replace("?", entryValue);
                sql.append(Task.ID).append(" IN (").append(subSql).append(") ");
            }

            if(instance.criterion.valuesForNewTasks != null &&
                    instance.type == CriterionInstance.TYPE_INTERSECT) {
                for(Entry<String, Object> entry : instance.criterion.valuesForNewTasks.valueSet()) {
                    values.put(entry.getKey().replace("?", entryValue),
                            entry.getValue().toString().replace("?", entryValue));
                }
                values.putAll(instance.criterion.valuesForNewTasks);
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
            if(instance.selectedIndex < 0 && instance.criterion.entryValues != null) {
                instance.start = last;
                instance.end = last;
                continue;
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
            if(instance.criterion.sql == null)
                sql.append(TaskCriteria.activeAndVisible()).append(' ');
            else {
                String subSql = instance.criterion.sql.replace("?",
                        instance.criterion.entryValues[instance.selectedIndex]);
                subSql = PermaSql.replacePlaceholders(subSql);
                System.err.println(subSql);
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

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if(item.getGroupId() == MENU_GROUP_FILTER_OPTION) {
            if(menuItemInstance == null) {
                CustomFilterCriterion criterion = criteria.get(item.getItemId());
                menuItemInstance = new CriterionInstance();
                menuItemInstance.criterion = criterion;
            }

            menuItemInstance.selectedIndex = item.getOrder();
            if(adapter.getPosition(menuItemInstance) == -1)
                adapter.add(menuItemInstance);
            updateList();
            return true;
        }

        else if(item.getGroupId() == MENU_GROUP_CONTEXT_TYPE) {
            CriterionInstance instance = adapter.getItem(item.getOrder());
            instance.type = item.getItemId();
            updateList();
        }

        else if(item.getGroupId() == MENU_GROUP_CONTEXT_DELETE) {
            CriterionInstance instance = adapter.getItem(item.getOrder());
            adapter.remove(instance);
            updateList();
        }

        return super.onMenuItemSelected(featureId, item);
    }

}
