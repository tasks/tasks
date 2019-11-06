package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.HashMap;
import java.util.Map;
import org.tasks.R;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavTask;

public class CaldavFilter extends Filter {

  /** Parcelable Creator Object */
  public static final Parcelable.Creator<CaldavFilter> CREATOR =
      new Parcelable.Creator<CaldavFilter>() {

        /** {@inheritDoc} */
        @Override
        public CaldavFilter createFromParcel(Parcel source) {
          CaldavFilter item = new CaldavFilter();
          item.readFromParcel(source);
          return item;
        }

        /** {@inheritDoc} */
        @Override
        public CaldavFilter[] newArray(int size) {
          return new CaldavFilter[size];
        }
      };

  private CaldavCalendar calendar;

  private CaldavFilter() {
    super();
  }

  public CaldavFilter(CaldavCalendar calendar) {
    super(calendar.getName(), queryTemplate(calendar), getValuesForNewTask(calendar));
    this.calendar = calendar;
    tint = calendar.getColor();
    icon = calendar.getIcon();
  }

  private static QueryTemplate queryTemplate(CaldavCalendar caldavCalendar) {
    return new QueryTemplate()
        .join(getJoin())
        .where(
            Criterion.and(
                TaskDao.TaskCriteria.activeAndVisible(),
                Field.field("caldav_tasks.cd_deleted").eq(0),
                Field.field("caldav_tasks.cd_calendar").eq(caldavCalendar.getUuid())));
  }

  private static Map<String, Object> getValuesForNewTask(CaldavCalendar caldavCalendar) {
    Map<String, Object> result = new HashMap<>();
    result.put(CaldavTask.KEY, caldavCalendar.getUuid());
    return result;
  }

  public String getUuid() {
    return calendar.getUuid();
  }

  public CaldavCalendar getCalendar() {
    return calendar;
  }

  /** {@inheritDoc} */
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeParcelable(calendar, 0);
  }

  @Override
  protected void readFromParcel(Parcel source) {
    super.readFromParcel(source);
    calendar = source.readParcelable(getClass().getClassLoader());
  }

  @Override
  public boolean supportsSubtasks() {
    return false;
  }

  @Override
  public int getMenu() {
    return R.menu.menu_caldav_list_fragment;
  }

  @Override
  public boolean areItemsTheSame(@NonNull FilterListItem other) {
    return other instanceof CaldavFilter
        && calendar.getUuid().equals(((CaldavFilter) other).getUuid());
  }

  @Override
  public boolean areContentsTheSame(@NonNull FilterListItem other) {
    return calendar.equals(((CaldavFilter) other).calendar);
  }

  private static Join getJoin() {
    return Join.left(CaldavTask.TABLE, Task.ID.eq(Field.field("caldav_tasks.cd_task")));
  }

  public static String getJoinSql() {
    return getJoin().toString();
  }
}
