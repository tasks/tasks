/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.todoroo.astrid.sync.SyncProviderUtilities;

import org.tasks.R;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Methods for working with GTasks preferences
 *
 * @author timsu
 *
 */
@Singleton
public class GtasksPreferenceService extends SyncProviderUtilities {

    /** add-on identifier */
    public static final String IDENTIFIER = "gtasks"; //$NON-NLS-1$

    @Inject
    public GtasksPreferenceService() {
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public int getSyncIntervalKey() {
        return R.string.gtasks_GPr_interval_key;
    }

    /** GTasks user's default list id */
    public static final String PREF_DEFAULT_LIST = IDENTIFIER + "_defaultlist"; //$NON-NLS-1$

    /** GTasks user name */
    public static final String PREF_USER_NAME = IDENTIFIER + "_user"; //$NON-NLS-1$
}
