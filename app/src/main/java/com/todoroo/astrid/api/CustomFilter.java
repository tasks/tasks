package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.tasks.R;

import java.util.Objects;

public class CustomFilter extends Filter {

  /** Parcelable Creator Object */
  public static final Parcelable.Creator<CustomFilter> CREATOR =
      new Parcelable.Creator<>() {

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

  private String criterion;

  public CustomFilter(@NonNull org.tasks.data.Filter filter) {
    super(filter.getTitle(), filter.getSql(), filter.getValuesAsMap());
    id = filter.getId();
    criterion = filter.getCriterion();
    tint = filter.getColor();
    icon = filter.getIcon();
    order = filter.getOrder();
  }

  private CustomFilter(Parcel parcel) {
    readFromParcel(parcel);
  }

  public String getCriterion() {
    return criterion;
  }

  /** {@inheritDoc} */
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(criterion);
  }

  @Override
  protected void readFromParcel(Parcel source) {
    super.readFromParcel(source);
    criterion = source.readString();
  }

  @Override
  public int getMenu() {
    return getId() > 0 ? R.menu.menu_custom_filter : 0;
  }

  @Override
  public boolean areContentsTheSame(@NonNull FilterListItem other) {
    return super.areContentsTheSame(other)
        && Objects.equals(criterion, ((CustomFilter) other).criterion);
  }
}
