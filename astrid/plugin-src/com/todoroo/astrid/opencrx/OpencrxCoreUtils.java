/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.opencrx;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.sync.SyncProviderUtilities;

public class OpencrxCoreUtils extends SyncProviderUtilities{

    public static final String OPENCRX_ACTIVITY_METADATA_KEY = "opencrx"; //$NON-NLS-1$

    public static final String IDENTIFIER = "crx"; //$NON-NLS-1$

    public static final LongProperty ACTIVITY_ID = new LongProperty(Metadata.TABLE, Metadata.VALUE1.name);
    public static final LongProperty ACTIVITY_CREATOR_ID = new LongProperty(Metadata.TABLE, Metadata.VALUE2.name);
    public static final LongProperty ACTIVITY_USERCREATOR_ID = new LongProperty(Metadata.TABLE, Metadata.VALUE3.name);
    public static final LongProperty ACTIVITY_ASSIGNED_TO_ID = new LongProperty(Metadata.TABLE, Metadata.VALUE4.name);
    public static final StringProperty ACTIVITY_CRX_ID = new StringProperty(Metadata.TABLE, Metadata.VALUE5.name);

    private static final String PREF_USER_ID = "crx_userid"; //$NON-NLS-1$
    private static final String PREF_DEFAULT_CREATOR = "opencrx_defaultcreator"; //$NON-NLS-1$
    private static final String PREFS_FILE = "crx-prefs"; //$NON-NLS-1$

    private static final String OPENCRX_PACKAGE = "ru.otdelit.astrid.opencrx"; //$NON-NLS-1$

    public static final long CREATOR_NO_SYNC = -1;

    public static final OpencrxCoreUtils INSTANCE = new OpencrxCoreUtils();

    private OpencrxCoreUtils(){
        // prevent instantiation
    }

    public Metadata newMetadata(long forTask) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, OPENCRX_ACTIVITY_METADATA_KEY);
        metadata.setValue(Metadata.TASK, forTask);
        metadata.setValue(ACTIVITY_ID, 0L);
        metadata.setValue(ACTIVITY_CREATOR_ID, getDefaultCreator());
        metadata.setValue(ACTIVITY_USERCREATOR_ID, getDefaultAssignedUser());
        metadata.setValue(ACTIVITY_ASSIGNED_TO_ID, getDefaultAssignedUser());
        metadata.setValue(ACTIVITY_CRX_ID, ""); //$NON-NLS-1$
        return metadata;
    }

    @Override
    public void stopOngoing() {
        SharedPreferences sharedPreferences = OpencrxCoreUtils.getPrefs();

        if (sharedPreferences != null){
            Editor editor = sharedPreferences.edit();
            editor.putBoolean(getIdentifier() + PREF_ONGOING, false);
            editor.commit();
        }
    }

    /**
     * Gets default creator from setting
     * @return CREATOR_NO_SYNC if should not sync, otherwise remote id
     */
    public long getDefaultCreator() {
        long defaultCreatorId = CREATOR_NO_SYNC ;
        SharedPreferences sharedPreferences = OpencrxCoreUtils.getPrefs();

        if (sharedPreferences != null){
            String defCreatorString = sharedPreferences.getString(PREF_DEFAULT_CREATOR, String.valueOf(CREATOR_NO_SYNC));

            try{
                defaultCreatorId = Long.parseLong(defCreatorString);
            }catch(Exception ex){
                defaultCreatorId = CREATOR_NO_SYNC;
            }
        }

        return defaultCreatorId;
    }

    public long getDefaultAssignedUser(){
        SharedPreferences sharedPreferences = OpencrxCoreUtils.getPrefs();

        if (sharedPreferences != null){
            return sharedPreferences.getLong(PREF_USER_ID, -1);
        }else{
            return -1;
        }
    }

    protected static SharedPreferences getPrefs() {
        try {
            Context crxContext = ContextManager.getContext().createPackageContext(OPENCRX_PACKAGE, 0);
            SharedPreferences sharedPreferences = crxContext.getSharedPreferences(PREFS_FILE,
                                            Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
            return sharedPreferences;
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public boolean isLoggedIn() {
        SharedPreferences sharedPreferences = OpencrxCoreUtils.getPrefs();

        if (sharedPreferences != null)
            return sharedPreferences.getString(getIdentifier() + PREF_TOKEN, null) != null;
        else
            return false;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
    @Override
    public int getSyncIntervalKey() {
        return 0;
    }

    @Override
    public String getLoggedInUserName() {
        return ""; //$NON-NLS-1$
    }
}
