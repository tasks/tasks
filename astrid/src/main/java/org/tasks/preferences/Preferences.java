package org.tasks.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.utility.AstridDefaultPreferenceSpec;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;

public class Preferences {

    private static final String P_CURRENT_VERSION = "cv"; //$NON-NLS-1$
    private static final String P_CURRENT_VERSION_NAME = "cvname"; //$NON-NLS-1$

    private final Context context;
    private final SharedPreferences prefs;

    @Inject
    public Preferences(@ForApplication Context context) {
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setIfUnset(SharedPreferences prefs, SharedPreferences.Editor editor, Resources r, int keyResource, int value) {
        String key = r.getString(keyResource);
        if(!prefs.contains(key)) {
            editor.putString(key, Integer.toString(value));
        }
    }

    public void setIfUnset(SharedPreferences prefs, SharedPreferences.Editor editor, Resources r, int keyResource, boolean value) {
        String key = r.getString(keyResource);
        if(!prefs.contains(key) || !(prefs.getAll().get(key) instanceof Boolean)) {
            editor.putBoolean(key, value);
        }
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    public void clear() {
        prefs
                .edit()
                .clear()
                .commit();
    }

    public void setDefaults() {
        new AstridDefaultPreferenceSpec(context, this).setIfUnset();
    }

    public void reset() {
        clear();
        setDefaults();
    }

    public boolean isSet(String key) {
        return prefs.contains(key);
    }

    public String getStringValue(String key) {
        return prefs.getString(key, null);
    }

    public String getStringValue(int keyResource) {
        return prefs.getString(context.getResources().getString(keyResource), null);
    }

    public int getIntegerFromString(int keyResource, int defaultValue) {
        Resources r = context.getResources();
        String value = prefs.getString(r.getString(keyResource), null);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void setString(int resourceId, String value) {
        setString(ContextManager.getContext().getString(resourceId), value);
    }

    public void setString(String key, String newValue) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, newValue);
        editor.commit();
    }

    public void setStringFromInteger(int keyResource, int newValue) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(keyResource), Integer.toString(newValue));
        editor.commit();
    }

    public boolean getBoolean(String key, boolean defValue) {
        try {
            return prefs.getBoolean(key, defValue);
        } catch (ClassCastException e) {
            return defValue;
        }
    }

    public boolean getBoolean(int keyResources) {
        return getBoolean(keyResources, false);
    }

    public boolean getBoolean(int keyResources, boolean defValue) {
        return getBoolean(context.getString(keyResources), defValue);
    }

    public void setBoolean(int keyResource, boolean value) {
        setBoolean(context.getString(keyResource), value);
    }

    public void setBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public int getInt(int resourceId) {
        return getInt(resourceId, 0);
    }

    public int getInt(int resourceId, int defValue) {
        return getInt(ContextManager.getContext().getString(resourceId), defValue);
    }

    public int getInt(String key, int defValue) {
        return prefs.getInt(key, defValue);
    }

    public void setInt(int resourceId, int value) {
        setInt(ContextManager.getContext().getString(resourceId), value);
    }

    public void setInt(String key, int value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public long getLong(String key, long defValue) {
        return prefs.getLong(key, defValue);
    }

    public void setLong(String key, long value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public void clear(String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.commit();
    }

    public int getCurrentVersion() {
        return getInt(P_CURRENT_VERSION, 0);
    }

    public void setCurrentVersion(int version) {
        setInt(P_CURRENT_VERSION, version);
    }

    public void setCurrentVersionName(String versionName) {
        setString(P_CURRENT_VERSION_NAME, versionName);
    }
}
