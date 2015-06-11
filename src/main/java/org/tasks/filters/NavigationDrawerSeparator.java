package org.tasks.filters;

import com.todoroo.astrid.api.FilterListItem;

public class NavigationDrawerSeparator extends FilterListItem {

    @Override
    public Type getItemType() {
        return Type.SEPARATOR;
    }
}
