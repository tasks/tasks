package com.todoroo.astrid.core;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.CustomFilterCriterion;
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

    // --- hierarchy of filter classes

    public static class CriterionInstance {
        public static final int TYPE_ADD = 0;
        public static final int TYPE_SUBTRACT = 1;
        public static final int TYPE_INTERSECT = 2;
        public static final int TYPE_UNIVERSE = 3;

        /** criteria for this instance */
        public CustomFilterCriterion criterion;

        /** which of the entries is selected */
        public int selectedIndex;

        /** type of join */
        public int type;

        /** statistics for {@link FilterView} */
        public int start, end, max;
    }

    private CustomFilterAdapter adapter;
    private final ArrayList<CustomFilterCriterion> criteria =
        new ArrayList<CustomFilterCriterion>();

    // --- activity

    @Autowired
    Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.custom_filter_activity);
        setTitle(R.string.CFA_title);

        DependencyInjectionService.getInstance().inject(this);
        populateCriteria();

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
                Query.select(Task.ID).from(Task.TABLE).where(Task.DUE_DATE.lte("%s")).toString(),
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
        values = new ContentValues();
        values.put(Task.IMPORTANCE.name, "%s");
        criterion = new CustomFilterCriterion(
                getString(R.string.CFC_importance_text),
                Query.select(Task.ID).from(Task.TABLE).where(Task.IMPORTANCE.lte("%s")).toString(),
                values, r.getStringArray(R.array.EPr_default_importance),
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
                Query.select(Metadata.TASK).from(Metadata.TABLE).where(Criterion.and(
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
                getListView().showContextMenu();
            }
        });

        getListView().setOnCreateContextMenuListener(this);
    }

    // --- listeners and action events

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        for(int i = 0; i < criteria.size(); i++) {
            CustomFilterCriterion item = criteria.get(i);
            MenuItem menuItem = menu.add(MENU_GROUP_FILTER, i, i, item.name);
            if(item.icon != null)
                menuItem.setIcon(new BitmapDrawable(item.icon));
        }
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
            else
                sql.append(Task.ID).append(" IN (").append(instance.criterion.sql).append(") ");

            Cursor cursor = database.getDatabase().rawQuery(sql.toString(), null);
            try {
                cursor.moveToNext();
                max = Math.max(max, cursor.getCount());
                instance.start = last == -1 ? cursor.getInt(0) : last;
                instance.end = cursor.getInt(0);
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
        if(item.getGroupId() == MENU_GROUP_FILTER) {
            CustomFilterCriterion criterion = criteria.get(item.getItemId());
            CriterionInstance instance = new CriterionInstance();
            instance.criterion = criterion;
            adapter.add(instance);
            return true;
        }

        else if(item.getGroupId() == MENU_GROUP_FILTER_OPTION)
            return adapter.onMenuItemSelected(item);


        return super.onMenuItemSelected(featureId, item);
    }

}
