package com.todoroo.astrid.api;

import static com.todoroo.andlib.utility.AndroidUtilities.mapToSerializedString;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Map;

public class CustomFilter extends Filter {

  /** Parcelable Creator Object */
  public static final Parcelable.Creator<CustomFilter> CREATOR =
      new Parcelable.Creator<CustomFilter>() {

        /** {@inheritDoc} */
        @Override
        public CustomFilter createFromParcel(Parcel source) {
          return new CustomFilter(source);
        }

        /** {@inheritDoc} */
        @Override
        public CustomFilter[] newArray(int size) {
          return new CustomFilter[size];
        }
      };

  private long id;
  private String criterion;

  public CustomFilter(
      String listingTitle, String sql, Map<String, Object> values, long id, String criterion) {
    super(listingTitle, sql, values);
    this.id = id;
    this.criterion = criterion;
  }

  private CustomFilter(Parcel parcel) {
    readFromParcel(parcel);
  }

  public org.tasks.data.Filter toStoreObject() {
    org.tasks.data.Filter filter = new org.tasks.data.Filter();
    filter.setId(id);
    filter.setTitle(listingTitle);
    filter.setSql(sqlQuery);
    if (valuesForNewTasks != null && valuesForNewTasks.size() > 0) {
      filter.setValues(mapToSerializedString(valuesForNewTasks));
    }
    filter.setCriterion(criterion);
    return filter;
  }

  public long getId() {
    return id;
  }

  /** {@inheritDoc} */
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeLong(id);
    dest.writeString(criterion);
  }

  @Override
  protected void readFromParcel(Parcel source) {
    super.readFromParcel(source);
    id = source.readLong();
    criterion = source.readString();
  }
}
