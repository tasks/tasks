/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.weloveastrid.rmilk.MilkPreferences;
import org.weloveastrid.rmilk.MilkUtilities;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
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
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.andlib.widget.TodorooPreferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.MetadataHelper;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.voice.VoiceInputAssistant;
import com.todoroo.astrid.voice.VoiceOutputService;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditPreferences extends TodorooPreferences {

    private static final int POWER_PACK_PREFERENCE = 1;

    private static final String METADATA_CATEGORY = "category";//$NON-NLS-1$

    // --- instance variables

    @Autowired private TaskService taskService; // for debugging
    @Autowired private AddOnService addOnService;

    @Autowired
    private Database database;

    private VoiceInputAssistant voiceInputAssistant;

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
        voiceInputAssistant = new VoiceInputAssistant(this);

        addPluginPreferences(screen);

        screen.getPreference(POWER_PACK_PREFERENCE).setEnabled(addOnService.hasPowerPack());

        addDebugPreferences();

        addPreferenceListeners();
    }

    private void addPluginPreferences(PreferenceScreen screen) {
        Intent queryIntent = new Intent(AstridApiConstants.ACTION_SETTINGS);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(queryIntent,
                PackageManager.GET_META_DATA);
        int length = resolveInfoList.size();
        LinkedHashMap<String, ArrayList<Preference>> categoryPreferences =
            new LinkedHashMap<String, ArrayList<Preference>>();

        // Loop through a list of all packages (including plugins, addons)
        // that have a settings action
        for(int i = 0; i < length; i++) {
            ResolveInfo resolveInfo = resolveInfoList.get(i);
            Intent intent = new Intent(AstridApiConstants.ACTION_SETTINGS);
            intent.setClassName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);

            if(MilkPreferences.class.getName().equals(resolveInfo.activityInfo.name) &&
                    !MilkUtilities.INSTANCE.isLoggedIn())
                continue;

            Preference preference = new Preference(this);
            preference.setTitle(resolveInfo.activityInfo.loadLabel(pm));
            preference.setIntent(intent);

            String category = MetadataHelper.resolveActivityCategoryName(resolveInfo, pm);

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
    public void updatePreferences(final Preference preference, Object value) {
        final Resources r = getResources();
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

        // statistics service
        else if (r.getString(R.string.p_statistics).equals(preference.getKey())) {
            if (value != null && !(Boolean)value)
                preference.setSummary(R.string.EPr_statistics_desc_disabled);
            else
                preference.setSummary(R.string.EPr_statistics_desc_enabled);
        }

        // voice input and output
        if(!addOnService.hasPowerPack())
            return;

        if (r.getString(R.string.p_voiceInputEnabled).equals(preference.getKey())) {
            if (value != null && !(Boolean)value)
                preference.setSummary(R.string.EPr_voiceInputEnabled_desc_disabled);
            else
                preference.setSummary(R.string.EPr_voiceInputEnabled_desc_enabled);
            onVoiceInputStatusChanged(preference, (Boolean)value);
        } else if (r.getString(R.string.p_voiceRemindersEnabled).equals(preference.getKey())) {
            if (value != null && !(Boolean)value)
                preference.setSummary(R.string.EPr_voiceRemindersEnabled_desc_disabled);
            else
                preference.setSummary(R.string.EPr_voiceRemindersEnabled_desc_enabled);
            onVoiceReminderStatusChanged(preference, (Boolean)value);
        } else if (r.getString(R.string.p_voiceInputCreatesTask).equals(preference.getKey())) {
            if (value != null && !(Boolean)value)
                preference.setSummary(R.string.EPr_voiceInputCreatesTask_desc_disabled);
            else
                preference.setSummary(R.string.EPr_voiceInputCreatesTask_desc_enabled);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            VoiceOutputService.getVoiceOutputInstance().handleActivityResult(requestCode, resultCode, data);
        } catch (VerifyError e) {
            // unavailable
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void addPreferenceListeners() {
        //
    }


    private void onVoiceReminderStatusChanged(final Preference preference, boolean newValue) {
        try {
            VoiceOutputService.getVoiceOutputInstance();
            if(newValue)
                VoiceOutputService.getVoiceOutputInstance().checkIsTTSInstalled();
        } catch (VerifyError e) {
            // doesn't work :(
            preference.setEnabled(false);
            Preferences.setBoolean(preference.getKey(), false);
        }
    }

    private void onVoiceInputStatusChanged(final Preference preference, boolean newValue) {
        if(!newValue)
            return;

        final Resources r = getResources();
        if (!voiceInputAssistant.isVoiceInputAvailable()) {
            if (AndroidUtilities.getSdkVersion() > 6) {
                DialogUtilities.okCancelDialog(this,
                        r.getString(R.string.EPr_voiceInputInstall_dlg),
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                                // User wants to install voice search, take him to the market
                                Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("market://search?q=pname:" + //$NON-NLS-1$
                                                "com.google.android.voicesearch.x")); //$NON-NLS-1$
                                try {
                                    startActivity(marketIntent);
                                } catch (ActivityNotFoundException ane) {
                                    DialogUtilities.okDialog(EditPreferences.this,
                                            r.getString(R.string.EPr_marketUnavailable_dlg),
                                            new OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog1,
                                                        int which1) {
                                                    ((CheckBoxPreference)preference).setChecked(false);
                                                    dialog1.dismiss();
                                                }
                                            });
                                }
                            }
                        },
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                ((CheckBoxPreference)preference).setChecked(false);
                                dialog.dismiss();
                            }
                        });
            } else {
                DialogUtilities.okDialog(this,
                        r.getString(R.string.EPr_voiceInputUnavailable_dlg),
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog1,
                                    int which1) {
                                ((CheckBoxPreference)preference).setChecked(false);
                                dialog1.dismiss();
                            }
                        });
            }
        }
    }

}