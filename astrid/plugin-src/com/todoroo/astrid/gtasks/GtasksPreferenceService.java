/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * Methods for working with GTasks preferences
 *
 * @author timsu
 *
 */
public class GtasksPreferenceService extends SyncProviderUtilities {

    /** add-on identifier */
    public static final String IDENTIFIER = "gtasks"; //$NON-NLS-1$

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public int getSyncIntervalKey() {
        return R.string.gtasks_GPr_interval_key;
    }

    public boolean migrationHasOccurred() {
        return Preferences.getBoolean(PREF_MIGRATION_HAS_OCCURRED, false);
    }

    /** GTasks user's default list id */
    public static final String PREF_DEFAULT_LIST = IDENTIFIER + "_defaultlist"; //$NON-NLS-1$

    /** GTasks user name */
    public static final String PREF_USER_NAME = IDENTIFIER + "_user"; //$NON-NLS-1$

    /** GTasks is apps for domain boolean */
    public static final String PREF_IS_DOMAIN = IDENTIFIER + "_domain"; //$NON-NLS-1$

    /** GTasks whether we have shown list help boolean */
    public static final String PREF_SHOWN_LIST_HELP = IDENTIFIER + "_list_help"; //$NON-NLS-1$

    /** Pref that is set once migration to new GTasks API has occurred */
    public static final String PREF_MIGRATION_HAS_OCCURRED = IDENTIFIER + "_migrated"; //$NON-NLS-1$

    @Override
    public String getLoggedInUserName() {
        return Preferences.getStringValue(PREF_USER_NAME);
    }

    @Override
    protected void reportLastErrorImpl(String lastError, String type) {
        StatisticsService.reportEvent(StatisticsConstants.GTASKS_SYNC_ERROR, "type", type); //$NON-NLS-1$
    }

}
