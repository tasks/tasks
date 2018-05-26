package org.tasks.filters;

import android.os.Parcel;
import android.os.Parcelable;
import com.todoroo.astrid.api.FilterListItem;

public class NavigationDrawerSubheader extends FilterListItem {

  public static final Parcelable.Creator<NavigationDrawerSubheader> CREATOR =
      new Parcelable.Creator<NavigationDrawerSubheader>() {

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

  private NavigationDrawerSubheader() {}

  public NavigationDrawerSubheader(String listingTitle, boolean error) {
    this.error = error;
    this.listingTitle = listingTitle;
  }

  @Override
  protected void readFromParcel(Parcel source) {
    super.readFromParcel(source);
    error = source.readInt() == 1;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeInt(error ? 1 : 0);
  }

  @Override
  public Type getItemType() {
    return Type.SUBHEADER;
  }
}
