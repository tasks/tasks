/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Preferences;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditPreferences extends PreferenceActivity {

    @Autowired
    TaskService taskService; // for debugging

    @Autowired
    TaskDao taskDao;

    @Autowired
    Database database;

    @Autowired
    DialogUtilities dialogUtilities;

    public EditPreferences() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new StartupService().onStartupApplication(this);
        ContextManager.setContext(this);

        setTitle(R.string.EPr_title);
        addPreferencesFromResource(R.xml.preferences);

        PreferenceScreen screen = getPreferenceScreen();
        initializePreference(screen);

        // load plug-ins
        Intent queryIntent = new Intent(AstridApiConstants.ACTION_SETTINGS);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(queryIntent, 0);
        int length = resolveInfoList.size();
        LinkedHashMap<String, ArrayList<Preference>> applicationPreferences =
            new LinkedHashMap<String, ArrayList<Preference>>();
        for(int i = 0; i < length; i++) {
            ResolveInfo resolveInfo = resolveInfoList.get(i);
            Intent intent = new Intent(AstridApiConstants.ACTION_SETTINGS);
            intent.setClassName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);

            Preference preference = new Preference(this);
            preference.setTitle(resolveInfo.activityInfo.loadLabel(pm));
            preference.setIntent(intent);

            String application = resolveInfo.activityInfo.applicationInfo.loadLabel(pm).toString();
            if(!applicationPreferences.containsKey(application))
                applicationPreferences.put(application, new ArrayList<Preference>());
            ArrayList<Preference> arrayList = applicationPreferences.get(application);
            arrayList.add(preference);
        }

        for(Entry<String, ArrayList<Preference>> entry : applicationPreferences.entrySet()) {
            Preference header = new Preference(this);
            header.setLayoutResource(android.R.layout.preference_category);
            header.setTitle(entry.getKey());
            screen.addPreference(header);

            for(Preference preference : entry.getValue())
                screen.addPreference(preference);
        }

        // debugging preferences
        addDebugPreferences();
    }

    @SuppressWarnings("nls")
    private void addDebugPreferences() {
        if(!Constants.DEBUG)
            return;

        PreferenceCategory group = new PreferenceCategory(this);
        group.setTitle("DEBUG");
        getPreferenceScreen().addPreference(group);

        Preference preference = new Preference(this);
        preference.setTitle("Make Lots of Tasks");
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                database.openForWriting();
                Task task = new Task();
                for(int i = 0; i < 100; i++) {
                    task.setId(Task.NO_ID);
                    task.setValue(Task.TITLE, Integer.toString(i));
                    taskService.save(task, false);
                }
                dialogUtilities.okDialog(EditPreferences.this, "done", null);
                return false;
            }
        });
        group.addPreference(preference);

        preference = new Preference(this);
        preference.setTitle("Delete all tasks");
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                database.openForWriting();
                taskDao.deleteWhere(Criterion.all);
                dialogUtilities.okDialog(EditPreferences.this, "done", null);
                return false;
            }
        });
        group.addPreference(preference);
    }

    private void initializePreference(Preference preference) {
        if(preference instanceof PreferenceGroup) {
            PreferenceGroup group = (PreferenceGroup)preference;
            for(int i = 0; i < group.getPreferenceCount(); i++) {
                initializePreference(group.getPreference(i));
            }
        } else {
            Object value = null;
            if(preference instanceof ListPreference)
                value = ((ListPreference)preference).getValue();
            else if(preference instanceof CheckBoxPreference)
                value = ((CheckBoxPreference)preference).isChecked();
            else if(preference instanceof EditTextPreference)
                value = ((EditTextPreference)preference).getText();

            if(value != null)
                updatePreferences(preference, value);

            preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference myPreference, Object newValue) {
                    return updatePreferences(myPreference, newValue);
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     *
     * @param resource if null, updates all resources
     */
    protected boolean updatePreferences(Preference preference, Object value) {
        Resources r = getResources();

        // defaults options
        /*if(r.getString(R.string.EPr_default_urgency_key).equals(preference.getKey())) {
            updateTaskListPreference(preference, value, r, R.array.EPr_default_urgency,
                    R.array.EPr_default_urgency_values, R.string.EPr_default_urgency_desc);
        } else if(r.getString(R.string.EPr_default_importance_key).equals(preference.getKey())) {
            updateTaskListPreference(preference, value, r, R.array.EPr_default_importance,
                    R.array.EPr_default_importance_values, R.string.EPr_default_importance_desc);
        }*/

        return true;
    }

    private void updateTaskListPreference(Preference preference, Object value,
            Resources r, int keyArray, int valueArray, int summaryResource) {
        int index = AndroidUtilities.indexOf(r.getStringArray(valueArray), (String)value);
        String setting = r.getStringArray(keyArray)[index];
        preference.setSummary(r.getString(summaryResource,
                setting));

        // if user changed the value, refresh task defaults
        if(!value.equals(Preferences.getStringValue(preference.getKey()))) {
            Editor editor = Preferences.getPrefs(this).edit();
            editor.putString(preference.getKey(), (String)value);
            editor.commit();
            Task.refreshDefaultValues();
        }
    }
}