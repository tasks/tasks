package org.tasks.filters;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ParcelCompat;
import com.todoroo.astrid.api.FilterListItem;

public class NavigationDrawerSubheader extends FilterListItem {

  public static final Parcelable.Creator<NavigationDrawerSubheader> CREATOR =
      new Parcelable.Creator<>() {

        /** {@inheritDoc} */
        @Override
        public NavigationDrawerSubheader createFromParcel(Parcel source) {
          NavigationDrawerSubheader navigationDrawerSubheader = new NavigationDrawerSubheader();
          navigationDrawerSubheader.readFromParcel(source);
          return navigationDrawerSubheader;
        }

        /** {@inheritDoc} */
        @Override
        public NavigationDrawerSubheader[] newArray(int size) {
          return new NavigationDrawerSubheader[size];
        }
      };
  public boolean error;
  private boolean collapsed;
  private SubheaderType subheaderType;
  private long id;
  @Nullable
  private Intent addIntent;
  private int addIntentRc;

  private NavigationDrawerSubheader() {}

  public NavigationDrawerSubheader(
          String listingTitle,
          boolean error,
          boolean collapsed,
          SubheaderType subheaderType,
          long id,
          int addIntentRc,
          @Nullable Intent addIntent
  ) {
    this.error = error;
    this.collapsed = collapsed;
    this.subheaderType = subheaderType;
    this.id = id;
    this.addIntent = addIntent;
    this.addIntentRc = addIntentRc;
    this.listingTitle = listingTitle;
  }

  public long getId() {
    return id;
  }

  public boolean isCollapsed() {
    return collapsed;
  }

  @Nullable
  public Intent getAddIntent() {
    return addIntent;
  }

  public int getAddIntentRc() {
    return addIntentRc;
  }

  public SubheaderType getSubheaderType() {
    return subheaderType;
  }

  @Override
  protected void readFromParcel(Parcel source) {
    super.readFromParcel(source);
    error = ParcelCompat.readBoolean(source);
    collapsed = ParcelCompat.readBoolean(source);
    subheaderType = (SubheaderType) source.readSerializable();
    id = source.readLong();
    addIntent = source.readParcelable(getClass().getClassLoader());
    addIntentRc = source.readInt();
  }

  @Override
  public boolean areItemsTheSame(@NonNull FilterListItem other) {
    return other instanceof NavigationDrawerSubheader
        && subheaderType == ((NavigationDrawerSubheader) other).getSubheaderType()
        && id == other.getId();
  }

  @Override
  public boolean areContentsTheSame(@NonNull FilterListItem other) {
    return this.equals(other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NavigationDrawerSubheader)) {
      return false;
    }

    NavigationDrawerSubheader that = (NavigationDrawerSubheader) o;

    if (error != that.error) {
      return false;
    }
    if (collapsed != that.collapsed) {
      return false;
    }
    if (id != that.id) {
      return false;
    }
    return subheaderType == that.subheaderType;
  }

  @Override
  public int hashCode() {
    int result = (error ? 1 : 0);
    result = 31 * result + (collapsed ? 1 : 0);
    result = 31 * result + (subheaderType != null ? subheaderType.hashCode() : 0);
    result = 31 * result + (int) (id ^ (id >>> 32));
    return result;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    ParcelCompat.writeBoolean(dest, error);
    ParcelCompat.writeBoolean(dest, collapsed);
    dest.writeSerializable(subheaderType);
    dest.writeLong(id);
    dest.writeParcelable(addIntent, 0);
    dest.writeInt(addIntentRc);
  }

  @Override
  public Type getItemType() {
    return Type.SUBHEADER;
  }

  public enum SubheaderType {
    PREFERENCE,
    GOOGLE_TASKS,
    CALDAV,
    TASKS,
    @Deprecated ETESYNC
  }
}
