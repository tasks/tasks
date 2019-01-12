package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;
import com.todoroo.andlib.sql.QueryTemplate;

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

  public SearchFilter(String title, QueryTemplate where) {
    super(title, where);
  }
}
