/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

/**
 * Common interface for Astrids filter-exposers to provide their {@link FilterListitem}-instances.
 *
 * @author Arne Jans
 */
public interface AstridFilterExposer {
    public FilterListItem[] getFilters();
}
