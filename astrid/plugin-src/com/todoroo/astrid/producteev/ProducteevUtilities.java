package com.todoroo.astrid.producteev;

import com.timsu.astrid.R;
import com.todoroo.astrid.common.SyncProviderUtilities;
import com.todoroo.astrid.utility.Preferences;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class ProducteevUtilities extends SyncProviderUtilities {

    /** add-on identifier */
    public static final String IDENTIFIER = "pdv"; //$NON-NLS-1$

    public static final ProducteevUtilities INSTANCE = new ProducteevUtilities();

    /** setting for dashboard to not synchronize */
    public static final int DASHBOARD_NO_SYNC = -1;

    /** setting for dashboard to use default one */
    public static final int DASHBOARD_DEFAULT = 0;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public int getSyncIntervalKey() {
        return R.string.producteev_PPr_interval_key;
    }

    // --- producteev-specific preferences

    public static final String PREF_SERVER_LAST_SYNC = IDENTIFIER + "_last_server"; //$NON-NLS-1$

    public static final String PREF_EMAIL = IDENTIFIER + "_email"; //$NON-NLS-1$

    public static final String PREF_PASSWORD = IDENTIFIER + "_password"; //$NON-NLS-1$

    public static final String PREF_DEFAULT_DASHBOARD = IDENTIFIER + "_defaultdash"; //$NON-NLS-1$

    public static final String PREF_USER_ID = IDENTIFIER + "_userid"; //$NON-NLS-1$

    /**
     * Gets default dashboard from setting
     * @return DASHBOARD_NO_SYNC if should not sync, otherwise remote id
     */
    public long getDefaultDashboard() {
        int defaultDashboard = Preferences.getIntegerFromString(R.string.producteev_PPr_defaultdash_key,
                DASHBOARD_DEFAULT);
        if(defaultDashboard == DASHBOARD_NO_SYNC)
            return DASHBOARD_NO_SYNC;
        else if(defaultDashboard == DASHBOARD_DEFAULT)
            return Preferences.getLong(PREF_DEFAULT_DASHBOARD, 0);
        else
            return (long) defaultDashboard;
    }

    private ProducteevUtilities() {
        // prevent instantiation
    }

}