package org.tasks.filters;

import android.os.Parcel;
import android.os.Parcelable;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;

public class NoSubtasksFilter extends Filter {

  public static final Parcelable.Creator<NoSubtasksFilter> CREATOR =
      new Parcelable.Creator<NoSubtasksFilter>() {

        /** {@inheritDoc} */
        @Override
        public NoSubtasksFilter createFromParcel(Parcel source) {
          NoSubtasksFilter item = new NoSubtasksFilter();
          item.readFromParcel(source);
          return item;
        }

        /** {@inheritDoc} */
        @Override
        public NoSubtasksFilter[] newArray(int size) {
          return new NoSubtasksFilter[size];
        }
      };

  public NoSubtasksFilter(String listingTitle, QueryTemplate sqlQuery) {
    super(listingTitle, sqlQuery);
  }

  private NoSubtasksFilter() {}

  @Override
  public boolean supportsSubtasks() {
    return false;
  }
}
