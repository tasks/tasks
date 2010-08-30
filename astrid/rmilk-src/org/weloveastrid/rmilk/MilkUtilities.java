/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * Constants and preferences for RTM plugin
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class MilkUtilities extends SyncProviderUtilities {

    // --- constants

    /** add-on identifier */
    public static final String IDENTIFIER = "rmilk";

    public static final MilkUtilities INSTANCE = new MilkUtilities();

    // --- utilities boilerplate

    private MilkUtilities() {
        // if no token is set, see if astrid has exported one
        if(getToken() == null) {
            try {
                Context astridContext = ContextManager.getContext().createPackageContext("com.timsu.astrid", 0);
                SharedPreferences sharedPreferences = astridContext.getSharedPreferences("rtm", 0);
                if(sharedPreferences != null) {
                    String token = sharedPreferences.getString("rmilk_token", null);
                    long lastSyncDate = sharedPreferences.getLong("rmilk_last_sync", 0);

                    Editor editor = getPrefs().edit();
                    editor.putString(getIdentifier() + PREF_TOKEN, token);
                    editor.putLong(getIdentifier() + PREF_LAST_SYNC, lastSyncDate);
                    editor.commit();
                }
            } catch (Exception e) {
                // too bad
            }
        }
    }

;    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public int getSyncIntervalKey() {
        return R.string.rmilk_MPr_interval_key;
    }

}
