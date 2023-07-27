package org.tasks.filters;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.todoroo.astrid.api.FilterListItem;

public class NavigationDrawerSeparator extends FilterListItem {

  public static final Parcelable.Creator<NavigationDrawerSeparator> CREATOR =
      new Parcelable.Creator<>() {

        /** {@inheritDoc} */
        @Override
        public NavigationDrawerSeparator createFromParcel(Parcel source) {
          NavigationDrawerSeparator navigationDrawerSeparator = new NavigationDrawerSeparator();
          navigationDrawerSeparator.readFromParcel(source);
          return navigationDrawerSeparator;
        }

        /** {@inheritDoc} */
        @Override
        public NavigationDrawerSeparator[] newArray(int size) {
          return new NavigationDrawerSeparator[size];
        }
      };

  @Override
  public Type getItemType() {
    return Type.SEPARATOR;
  }

  @Override
  public boolean areItemsTheSame(@NonNull FilterListItem other) {
    return other instanceof NavigationDrawerSeparator;
  }

  @Override
  public boolean areContentsTheSame(@NonNull FilterListItem other) {
    return true;
  }
}
