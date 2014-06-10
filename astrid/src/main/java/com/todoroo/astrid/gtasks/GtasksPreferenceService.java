/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.todoroo.astrid.sync.SyncProviderUtilities;

import org.tasks.R;
import org.tasks.preferences.Preferences;

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

    public static final String IDENTIFIER = "gtasks"; //$NON-NLS-1$

    private static final String PREF_DEFAULT_LIST = IDENTIFIER + "_defaultlist"; //$NON-NLS-1$
    private static final String PREF_USER_NAME = IDENTIFIER + "_user"; //$NON-NLS-1$

    @Inject
    public GtasksPreferenceService(Preferences preferences) {
        super(preferences);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public int getSyncIntervalKey() {
        return R.string.gtasks_GPr_interval_key;
    }

    public String getDefaultList() {
        return preferences.getStringValue(PREF_DEFAULT_LIST);
    }

    public void setDefaultList(String defaultList) {
        preferences.setString(PREF_DEFAULT_LIST, defaultList);
    }

    public String getUserName() {
        return preferences.getStringValue(PREF_USER_NAME);
    }

    public void setUserName(String userName) {
        preferences.setString(PREF_USER_NAME, userName);
    }
}
