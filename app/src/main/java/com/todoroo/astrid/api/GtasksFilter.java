package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.HashMap;
import java.util.Map;
import org.tasks.R;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskList;

public class GtasksFilter extends Filter {

  /** Parcelable Creator Object */
  public static final Parcelable.Creator<GtasksFilter> CREATOR =
      new Parcelable.Creator<GtasksFilter>() {

        /** {@inheritDoc} */
        @Override
        public GtasksFilter createFromParcel(Parcel source) {
          GtasksFilter item = new GtasksFilter();
          item.readFromParcel(source);
          return item;
        }

        /** {@inheritDoc} */
        @Override
        public GtasksFilter[] newArray(int size) {
          return new GtasksFilter[size];
        }
      };

  private static final int CLOUD = R.drawable.ic_outline_cloud_24px;
  private GoogleTaskList list;

  private GtasksFilter() {
    super();
  }

  public GtasksFilter(GoogleTaskList list) {
    super(list.getTitle(), getQueryTemplate(list), getValuesForNewTasks(list));
    this.list = list;
    tint = list.getColor();
    icon = CLOUD;
  }

  public static String toManualOrder(String query) {
    query =
        query.replace(
            "WHERE",
            "JOIN (SELECT google_tasks.*, COUNT(c.gt_id) AS children, 0 AS siblings, google_tasks.gt_order AS primary_sort, NULL AS secondary_sort FROM google_tasks LEFT JOIN google_tasks AS c ON c.gt_parent = google_tasks.gt_task WHERE google_tasks.gt_parent = 0 GROUP BY google_tasks.gt_task UNION SELECT c.*, 0 AS children, COUNT(s.gt_id) AS siblings, p.gt_order AS primary_sort, c.gt_order AS secondary_sort FROM google_tasks AS c LEFT JOIN google_tasks AS p ON c.gt_parent = p.gt_task LEFT JOIN tasks ON c.gt_parent = tasks._id LEFT JOIN google_tasks AS s ON s.gt_parent = p.gt_task WHERE c.gt_parent > 0 AND ((tasks.completed=0) AND (tasks.deleted=0) AND (tasks.hideUntil<(strftime('%s','now')*1000))) GROUP BY c.gt_task) as g2 ON g2.gt_id = google_tasks.gt_id WHERE");
    query = query.replaceAll("ORDER BY .*", "");
    query = query + "ORDER BY primary_sort ASC, secondary_sort ASC";
    return query;
  }

  private static QueryTemplate getQueryTemplate(GoogleTaskList list) {
    return new QueryTemplate()
        .join(Join.left(GoogleTask.TABLE, Task.ID.eq(Field.field("google_tasks.gt_task"))))
        .where(
            Criterion.and(
                TaskDao.TaskCriteria.activeAndVisible(),
                Field.field("google_tasks.gt_deleted").eq(0),
                Field.field("google_tasks.gt_list_id").eq(list.getRemoteId())));
  }

  private static Map<String, Object> getValuesForNewTasks(GoogleTaskList list) {
    Map<String, Object> values = new HashMap<>();
    values.put(GoogleTask.KEY, list.getRemoteId());
    return values;
  }

  public long getStoreId() {
    return list.getId();
  }

  public GoogleTaskList getList() {
    return list;
  }

  @Override
  public boolean supportsSubtasks() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeParcelable(list, 0);
  }

  @Override
  protected void readFromParcel(Parcel source) {
    super.readFromParcel(source);
    list = source.readParcelable(getClass().getClassLoader());
  }

  public String getRemoteId() {
    return list.getRemoteId();
  }

  @Override
  public int getMenu() {
    return R.menu.menu_gtasks_list_fragment;
  }
}
