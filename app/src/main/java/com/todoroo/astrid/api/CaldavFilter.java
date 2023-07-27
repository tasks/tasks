package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavTask;
import org.tasks.data.TaskDao;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CaldavFilter extends Filter {

  /** Parcelable Creator Object */
  public static final Parcelable.Creator<CaldavFilter> CREATOR =
      new Parcelable.Creator<>() {

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
    id = calendar.getId();
    tint = calendar.getColor();
    icon = calendar.getIcon();
    order = calendar.getOrder();
  }

  private static QueryTemplate queryTemplate(CaldavCalendar caldavCalendar) {
    return new QueryTemplate()
        .join(Join.left(CaldavTask.TABLE, Task.ID.eq(CaldavTask.TASK)))
        .where(getCriterion(caldavCalendar));
  }

  private static Criterion getCriterion(CaldavCalendar caldavCalendar) {
    return Criterion.and(
        TaskDao.TaskCriteria.activeAndVisible(),
        CaldavTask.DELETED.eq(0),
        CaldavTask.CALENDAR.eq(caldavCalendar.getUuid()));
  }

  private static Map<String, Object> getValuesForNewTask(CaldavCalendar caldavCalendar) {
    Map<String, Object> result = new HashMap<>();
    result.put(CaldavTask.KEY, caldavCalendar.getUuid());
    return result;
  }

  public String getUuid() {
    return calendar.getUuid();
  }

  public String getAccount() {
    return calendar.getAccount();
  }

  public CaldavCalendar getCalendar() {
    return calendar;
  }

  @Override
  public boolean isReadOnly() {
    return calendar.getAccess() == CaldavCalendar.ACCESS_READ_ONLY;
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
  public boolean supportsManualSort() {
    return true;
  }

  @Override
  public int getMenu() {
    return R.menu.menu_caldav_list_fragment;
  }

  @Override
  public boolean areContentsTheSame(@NonNull FilterListItem other) {
    return super.areContentsTheSame(other)
        && Objects.equals(calendar, ((CaldavFilter) other).calendar);
  }
}
