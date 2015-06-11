package org.tasks.filters;

import com.todoroo.astrid.api.FilterListItem;

public class NavigationDrawerSubheader extends FilterListItem {

    public NavigationDrawerSubheader(String listingTitle) {
        this.listingTitle = listingTitle;
    }

    @Override
    public Type getItemType() {
        return Type.SUBHEADER;
    }
}
