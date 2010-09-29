/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.widget.TodorooPreferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.andlib.utility.Preferences;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditPreferences extends TodorooPreferences {

    private static final String METADATA_CATEGORY = "category";//$NON-NLS-1$

    // --- instance variables

    @Autowired
    private TaskService taskService; // for debugging

    @Autowired
    private Database database;

    public EditPreferences() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new StartupService().onStartupApplication(this);
        ContextManager.setContext(this);

        PreferenceScreen screen = getPreferenceScreen();

        // load plug-ins
        Intent queryIntent = new Intent(AstridApiConstants.ACTION_SETTINGS);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(queryIntent,
                PackageManager.GET_META_DATA);
        int length = resolveInfoList.size();
        LinkedHashMap<String, ArrayList<Preference>> categoryPreferences =
            new LinkedHashMap<String, ArrayList<Preference>>();
        for(int i = 0; i < length; i++) {
            ResolveInfo resolveInfo = resolveInfoList.get(i);
            Intent intent = new Intent(AstridApiConstants.ACTION_SETTINGS);
            intent.setClassName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);

            Preference preference = new Preference(this);
            preference.setTitle(resolveInfo.activityInfo.loadLabel(pm));
            preference.setIntent(intent);

            // category - either from metadata, or the application name
            String category = null;
            if(resolveInfo.activityInfo.metaData != null &&
                    resolveInfo.activityInfo.metaData.containsKey(METADATA_CATEGORY)) {
                int resource = resolveInfo.activityInfo.metaData.getInt(METADATA_CATEGORY, -1);
                if(resource > -1) {
                    try {
                        category = pm.getResourcesForApplication(resolveInfo.activityInfo.applicationInfo).getString(resource);
                    } catch (Exception e) {
                        //
                    }
                } else {
                    category = resolveInfo.activityInfo.metaData.getString(METADATA_CATEGORY);
                }
            }
            if(category == null)
                category = resolveInfo.activityInfo.applicationInfo.loadLabel(pm).toString();

            if(!categoryPreferences.containsKey(category))
                categoryPreferences.put(category, new ArrayList<Preference>());
            ArrayList<Preference> arrayList = categoryPreferences.get(category);
            arrayList.add(preference);
        }

        for(Entry<String, ArrayList<Preference>> entry : categoryPreferences.entrySet()) {
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
        preference.setTitle("Flush detail cache");
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                database.openForWriting();
                Toast.makeText(EditPreferences.this, "" + taskService.clearDetails(Criterion.all),
                        Toast.LENGTH_LONG).show();
                return false;
            }
        });
        group.addPreference(preference);

        preference = new Preference(this);
        preference.setTitle("Make Lots of Tasks");
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                database.openForWriting();
                Task task = new Task();
                for(int i = 0; i < 100; i++) {
                    task.clear();
                    task.setValue(Task.TITLE, Integer.toString(i));
                    taskService.save(task);
                }
                DialogUtilities.okDialog(EditPreferences.this, "done", null);
                return false;
            }
        });
        group.addPreference(preference);

        preference = new Preference(this);
        preference.setTitle("Delete all tasks");
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                database.openForWriting();
                taskService.deleteWhere(Criterion.all);
                DialogUtilities.okDialog(EditPreferences.this, "done", null);
                return false;
            }
        });
        group.addPreference(preference);
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        Resources r = getResources();
        // auto
        if (r.getString(R.string.p_showNotes).equals(preference.getKey())) {
            if (value != null && !(Boolean)value)
                preference.setSummary(R.string.EPr_showNotes_desc_disabled);
            else
                preference.setSummary(R.string.EPr_showNotes_desc_enabled);
            if((Boolean)value != Preferences.getBoolean(preference.getKey(), false)) {
                taskService.clearDetails(Criterion.all);
                Flags.set(Flags.REFRESH);
            }
        }
    }


}