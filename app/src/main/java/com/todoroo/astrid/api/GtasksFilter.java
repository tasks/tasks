package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

  private GoogleTaskList list;

  private GtasksFilter() {
    super();
  }

  public GtasksFilter(GoogleTaskList list) {
    super(list.getTitle(), getQueryTemplate(list), getValuesForNewTasks(list));
    this.list = list;
    id = list.getId();
    tint = list.getColor();
    icon = list.getIcon();
    order = list.getOrder();
  }

  private static QueryTemplate getQueryTemplate(GoogleTaskList list) {
    return new QueryTemplate()
        .join(Join.left(GoogleTask.TABLE, Task.ID.eq(GoogleTask.TASK)))
        .where(
            Criterion.and(
                TaskDao.TaskCriteria.activeAndVisible(),
                GoogleTask.DELETED.eq(0),
                GoogleTask.LIST.eq(list.getRemoteId())));
  }

  private static Map<String, Object> getValuesForNewTasks(GoogleTaskList list) {
    Map<String, Object> values = new HashMap<>();
    values.put(GoogleTask.KEY, list.getRemoteId());
    return values;
  }

  public long getStoreId() {
    return list.getId();
  }

  public String getAccount() {
    return list.getAccount();
  }

  public GoogleTaskList getList() {
    return list;
  }

  @Override
  public boolean supportsManualSort() {
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

  @Override
  public boolean areContentsTheSame(@NonNull FilterListItem other) {
    return super.areContentsTheSame(other) && Objects.equals(list, ((GtasksFilter) other).list);
  }
}
