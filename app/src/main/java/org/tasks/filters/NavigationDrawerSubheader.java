package org.tasks.filters;

import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.astrid.api.FilterListItem;

public class NavigationDrawerSubheader extends FilterListItem {

    private NavigationDrawerSubheader() {

    }

    public NavigationDrawerSubheader(String listingTitle) {
        this.listingTitle = listingTitle;
    }

    @Override
    public Type getItemType() {
        return Type.SUBHEADER;
    }

    public static final Parcelable.Creator<NavigationDrawerSubheader> CREATOR = new Parcelable.Creator<NavigationDrawerSubheader>() {

        /**
         * {@inheritDoc}
         */
        @Override
        public NavigationDrawerSubheader createFromParcel(Parcel source) {
            NavigationDrawerSubheader navigationDrawerSubheader = new NavigationDrawerSubheader();
            navigationDrawerSubheader.readFromParcel(source);
            return navigationDrawerSubheader;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NavigationDrawerSubheader[] newArray(int size) {
            return new NavigationDrawerSubheader[size];
        }
    };
}
