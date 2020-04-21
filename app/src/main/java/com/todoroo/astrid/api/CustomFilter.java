package com.todoroo.astrid.api;

import static com.todoroo.andlib.utility.AndroidUtilities.mapToSerializedString;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.google.common.base.Objects;
import org.tasks.R;

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

  public CustomFilter(org.tasks.data.Filter filter) {
    super(filter.getTitle(), filter.getSql(), filter.getValuesAsMap());
    this.id = filter.getId();
    this.criterion = filter.getCriterion();
    this.tint = filter.getColor();
    this.icon = filter.getIcon();
  }

  private CustomFilter(Parcel parcel) {
    readFromParcel(parcel);
  }

  public org.tasks.data.Filter toStoreObject() {
    org.tasks.data.Filter filter = new org.tasks.data.Filter();
    filter.setId(id);
    filter.setTitle(listingTitle);
    filter.setSql(sqlQuery);
    filter.setIcon(icon);
    filter.setColor(tint);
    if (valuesForNewTasks != null && valuesForNewTasks.size() > 0) {
      filter.setValues(mapToSerializedString(valuesForNewTasks));
    }
    filter.setCriterion(criterion);
    return filter;
  }

  public long getId() {
    return id;
  }

  public String getCriterion() {
    return criterion;
  }

  public void setCriterion(String criterion) {
    this.criterion = criterion;
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

  @Override
  public int getMenu() {
    return getId() > 0 ? R.menu.menu_custom_filter : 0;
  }

  @Override
  public boolean areItemsTheSame(@NonNull FilterListItem other) {
    return other instanceof CustomFilter && id == ((CustomFilter) other).id;
  }

  @Override
  public boolean areContentsTheSame(@NonNull FilterListItem other) {
    return super.areContentsTheSame(other)
        && Objects.equal(criterion, ((CustomFilter) other).criterion);
  }
}
