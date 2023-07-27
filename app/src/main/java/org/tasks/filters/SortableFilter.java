package org.tasks.filters;

import android.os.Parcel;
import android.os.Parcelable;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;
import java.util.Map;

public class SortableFilter extends Filter {

  public static final Parcelable.Creator<SortableFilter> CREATOR =
      new Parcelable.Creator<>() {

        /** {@inheritDoc} */
        @Override
        public SortableFilter createFromParcel(Parcel source) {
          SortableFilter item = new SortableFilter();
          item.readFromParcel(source);
          return item;
        }

        /** {@inheritDoc} */
        @Override
        public SortableFilter[] newArray(int size) {
          return new SortableFilter[size];
        }
      };

  public SortableFilter(String listingTitle, QueryTemplate sqlQuery) {
    super(listingTitle, sqlQuery);
  }

  public SortableFilter(
      String listingTitle, QueryTemplate sqlQuery, Map<String, Object> valuesForNewTasks) {
    super(listingTitle, sqlQuery, valuesForNewTasks);
  }

  private SortableFilter() {}

  @Override
  public boolean supportsAstridSorting() {
    return true;
  }
}
