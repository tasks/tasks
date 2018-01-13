package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskList;

import java.util.HashMap;
import java.util.Map;

public class GtasksFilter extends Filter {

    private static final int CLOUD = R.drawable.ic_cloud_black_24dp;

    private long storeId;

    private GtasksFilter() {
        super();
    }

    public GtasksFilter(GoogleTaskList list) {
        super(list.getTitle(), getQueryTemplate(list), getValuesForNewTasks(list));
        storeId = list.getId();
        tint = list.getColor();
        icon = CLOUD;
    }

    public static String toManualOrder(String query) {
        query = query.replaceAll("ORDER BY .*", "");
        query = query + " ORDER BY `order` ASC";
        return query.replace(
                TaskDao.TaskCriteria.activeAndVisible().toString(),
                TaskDao.TaskCriteria.notDeleted().toString());
    }

    public long getStoreId() {
        return storeId;
    }

    private static QueryTemplate getQueryTemplate(GoogleTaskList list) {
        return new QueryTemplate()
                .join(Join.left(GoogleTask.TABLE, Task.ID.eq(Field.field("google_tasks.task"))))
                .where(Criterion.and(
                        TaskDao.TaskCriteria.activeAndVisible(),
                        Field.field("list_id").eq(list.getRemoteId())));
    }

    private static Map<String, Object> getValuesForNewTasks(GoogleTaskList list) {
        Map<String, Object> values = new HashMap<>();
        values.put(GoogleTask.KEY, list.getRemoteId());
        return values;
    }

    @Override
    public boolean supportsSubtasks() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(storeId);
    }

    @Override
    protected void readFromParcel(Parcel source) {
        super.readFromParcel(source);
        storeId = source.readLong();
    }

    /**
     * Parcelable Creator Object
     */
    public static final Parcelable.Creator<GtasksFilter> CREATOR = new Parcelable.Creator<GtasksFilter>() {

        /**
         * {@inheritDoc}
         */
        @Override
        public GtasksFilter createFromParcel(Parcel source) {
            GtasksFilter item = new GtasksFilter();
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public GtasksFilter[] newArray(int size) {
            return new GtasksFilter[size];
        }
    };
}
