package com.todoroo.astrid.core;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.Filter;
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
                CustomFilterCriterion.VALUE_EOD_YESTERDAY,
                CustomFilterCriterion.VALUE_EOD,
                CustomFilterCriterion.VALUE_EOD_TOMORROW,
                CustomFilterCriterion.VALUE_EOD_DAY_AFTER,
                CustomFilterCriterion.VALUE_EOD_NEXT_WEEK,
        };
        ContentValues values = new ContentValues();
        values.put(Task.DUE_DATE.name, "%s");
        CustomFilterCriterion criterion = new CustomFilterCriterion(
                getString(R.string.CFC_dueBefore_text),
                Query.select(Task.ID).from(Task.TABLE).where(
                        Criterion.and(
                                TaskCriteria.activeAndVisible(),
                                Task.DUE_DATE.gt(0),
                                Task.DUE_DATE.lte("%s"))).toString(),
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
        values.put(Task.IMPORTANCE.name, "%s");
        criterion = new CustomFilterCriterion(
                getString(R.string.CFC_importance_text),
                Query.select(Task.ID).from(Task.TABLE).where(
                        Criterion.and(TaskCriteria.activeAndVisible(),
                                Task.IMPORTANCE.lte("%s"))).toString(),
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
        values.put(TagService.TAG.name, "%s");
        criterion = new CustomFilterCriterion(
                getString(R.string.CFC_tag_text),
                Query.select(Metadata.TASK).from(Metadata.TABLE).join(Join.inner(
                            Task.TABLE, Metadata.TASK.eq(Task.ID))).where(Criterion.and(
                        TaskCriteria.activeAndVisible(),
                        MetadataCriteria.withKey(TagService.KEY),
                        TagService.TAG.eq("%s"))).toString(),
                values, tagNames, tagNames,
                ((BitmapDrawable)r.getDrawable(R.drawable.filter_tags1)).getBitmap(),
                getString(R.string.CFC_tag_name));
        criteria.add(criterion);
    }

    private CriterionInstance getStartingUniverse() {
        CriterionInstance instance = new CriterionInstance();
        instance.criterion = new CustomFilterCriterion(getString(R.string.CFA_universe_all),
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

        ((Button)findViewById(R.id.saveAndView)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndView();
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
        for(int i = 0; i < adapter.getCount(); i++) {
            CriterionInstance instance = adapter.getItem(i);
            if(instance.selectedIndex < 0 && instance.criterion.entryValues != null)
                continue;

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
                String subSql = instance.criterion.sql.replaceAll("%s",
                        instance.criterion.entryValues[instance.selectedIndex]);
                subSql = CustomFilterCriterion.replacePlaceholders(subSql);
                sql.append(Task.ID).append(" IN (").append(subSql).append(") ");
            }
        }

        String title;
        if(filterName.getText().length() > 0)
            title = filterName.getText().toString();
        else
            title = filterName.getHint().toString();

        ContentValues values = new ContentValues(); // TODO
        Filter filter = new Filter(title, title, null, values);
        filter.sqlQuery = sql.toString();

        // TODO save

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
        StringBuilder suggestedTitle = new StringBuilder();

        for(int i = 0; i < adapter.getCount(); i++) {
            CriterionInstance instance = adapter.getItem(i);
            if(instance.selectedIndex < 0 && instance.criterion.entryValues != null) {
                instance.start = last;
                instance.end = last;
                continue;
            }

            String entryTitle = "";
            if(instance.criterion.entryTitles != null) {
                entryTitle = instance.criterion.entryTitles[instance.selectedIndex];
            }
            String title = instance.criterion.text.replace("%s", entryTitle);

            switch(instance.type) {
            case CriterionInstance.TYPE_ADD:
                sql.append("OR ");
                title = getString(R.string.CFA_type_add) + " " + title;
                break;
            case CriterionInstance.TYPE_SUBTRACT:
                sql.append("AND NOT ");
                title = getString(R.string.CFA_type_subtract) + " " + title;
                break;
            case CriterionInstance.TYPE_INTERSECT:
                sql.append("AND ");
                break;
            case CriterionInstance.TYPE_UNIVERSE:
            }

            suggestedTitle.append(title).append(' ');

            // special code for all tasks universe
            if(instance.criterion.sql == null)
                sql.append(TaskCriteria.activeAndVisible()).append(' ');
            else {
                String subSql = instance.criterion.sql.replaceAll("%s",
                        instance.criterion.entryValues[instance.selectedIndex]);
                subSql = CustomFilterCriterion.replacePlaceholders(subSql);
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

        if(adapter.getCount() > 1 && filterName.getText().length() == 0)
            filterName.setHint(suggestedTitle);
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
