package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.data.Task;
import org.tasks.data.Tag;

public class SearchFilter extends Filter {

  /** Parcelable Creator Object */
  public static final Parcelable.Creator<SearchFilter> CREATOR =
      new Parcelable.Creator<SearchFilter>() {

        /** {@inheritDoc} */
        @Override
        public SearchFilter createFromParcel(Parcel source) {
          SearchFilter item = new SearchFilter();
          item.readFromParcel(source);
          return item;
        }

        /** {@inheritDoc} */
        @Override
        public SearchFilter[] newArray(int size) {
          return new SearchFilter[size];
        }
      };

  private SearchFilter() {}

  public SearchFilter(String title, String query) {
    super(title, getQueryTemplate(query));
  }

  private static QueryTemplate getQueryTemplate(String query) {
    return new QueryTemplate()
        .join(Join.left(Tag.TABLE, Tag.TASK.eq(Task.ID)))
        .where(
            Criterion.and(
                Task.DELETION_DATE.eq(0),
                Criterion.or(
                    Task.NOTES.like("%" + query + "%"),
                    Task.TITLE.like("%" + query + "%"),
                    Tag.NAME.like("%" + query + "%"))));
  }

  @Override
  public boolean supportsHiddenTasks() {
    return false;
  }
}
