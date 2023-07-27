package org.tasks.filters;

import static org.tasks.data.Alarm.TYPE_SNOOZE;

import android.os.Parcel;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Task;

import org.tasks.data.Alarm;

public class SnoozedFilter extends Filter {

  public static final Creator<SnoozedFilter> CREATOR =
      new Creator<>() {

        /** {@inheritDoc} */
        @Override
        public SnoozedFilter createFromParcel(Parcel source) {
          SnoozedFilter item = new SnoozedFilter();
          item.readFromParcel(source);
          return item;
        }

        /** {@inheritDoc} */
        @Override
        public SnoozedFilter[] newArray(int size) {
          return new SnoozedFilter[size];
        }
      };

  public SnoozedFilter(String listingTitle) {
    super(listingTitle, getQueryTemplate());
  }

  private SnoozedFilter() {}

  private static QueryTemplate getQueryTemplate() {
      return new QueryTemplate()
              .join(Join.inner(Alarm.TABLE, Task.ID.eq(Alarm.TASK)))
              .where(Criterion.and(Task.DELETION_DATE.lte(0), Alarm.TYPE.eq(TYPE_SNOOZE)));
  }

  @Override
  public boolean supportsHiddenTasks() {
    return false;
  }
}
