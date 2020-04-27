package com.todoroo.astrid.api;

import android.os.Parcel;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.data.Task;
import java.util.List;
import org.tasks.data.Tag;

public class IdListFilter extends Filter {

  /** Parcelable Creator Object */
  public static final Creator<IdListFilter> CREATOR =
      new Creator<IdListFilter>() {

        /** {@inheritDoc} */
        @Override
        public IdListFilter createFromParcel(Parcel source) {
          IdListFilter item = new IdListFilter();
          item.readFromParcel(source);
          return item;
        }

        /** {@inheritDoc} */
        @Override
        public IdListFilter[] newArray(int size) {
          return new IdListFilter[size];
        }
      };

  private IdListFilter() {}

  public IdListFilter(List<Long>ids) {
    super("", getQueryTemplate(ids));
  }

  private static QueryTemplate getQueryTemplate(List<Long> ids) {
    return new QueryTemplate()
        .join(Join.left(Tag.TABLE, Tag.TASK.eq(Task.ID)))
        .where(Task.ID.in(ids));
  }
}
