/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.FileExplore;
import com.todoroo.astrid.gcal.CalendarAlarmScheduler;
import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.helper.MetadataHelper;
import com.todoroo.astrid.service.MarketStrategy.AmazonMarketStrategy;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.utility.TodorooPreferenceActivity;
import com.todoroo.astrid.voice.VoiceInputAssistant;
import com.todoroo.astrid.voice.VoiceOutputAssistant;
import com.todoroo.astrid.voice.VoiceRecognizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.preferences.Preferences;
import org.tasks.widget.WidgetHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditPreferences extends TodorooPreferenceActivity {

    private static final Logger log = LoggerFactory.getLogger(EditPreferences.class);
    private static final int REQUEST_CODE_SYNC = 0;
    private static final int REQUEST_CODE_FILES_DIR = 2;
    private static final int REQUEST_CODE_TTS_CHECK = 2534;

    public static final int RESULT_CODE_THEME_CHANGED = 1;
    public static final int RESULT_CODE_PERFORMANCE_PREF_CHANGED = 3;

    // --- instance variables

    @Inject StartupService startupService;
    @Inject TaskService taskService;
    @Inject Preferences preferences;
    @Inject CalendarAlarmScheduler calendarAlarmScheduler;
    @Inject VoiceOutputAssistant voiceOutputAssistant;

    private VoiceInputAssistant voiceInputAssistant;

    private class SetResultOnPreferenceChangeListener implements OnPreferenceChangeListener {
        private final int resultCode;
        public SetResultOnPreferenceChangeListener(int resultCode) {
            this.resultCode = resultCode;
        }

        @Override
        public boolean onPreferenceChange(Preference p, Object newValue) {
            setResult(resultCode);
            updatePreferences(p, newValue);
            return true;
        }
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startupService.onStartupApplication(this);
        ContextManager.setContext(this);

        PreferenceScreen screen = getPreferenceScreen();
        voiceInputAssistant = new VoiceInputAssistant(this);

        addPluginPreferences(screen);

        addPreferencesFromResource(R.xml.preferences_misc);

        final Resources r = getResources();

        // first-order preferences

        Preference beastMode = findPreference(getString(R.string.p_beastMode));
        beastMode.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                showBeastMode();
                return true;
            }
        });

        Preference preference = screen.findPreference(getString(R.string.p_files_dir));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                Intent filesDir = new Intent(EditPreferences.this, FileExplore.class);
                filesDir.putExtra(FileExplore.EXTRA_DIRECTORIES_SELECTABLE, true);
                startActivityForResult(filesDir, REQUEST_CODE_FILES_DIR);
                return true;
            }
        });

        addPreferenceListeners();

        preference = screen.findPreference(getString(R.string.p_showNotes));
        preference.setEnabled(preferences.getIntegerFromString(R.string.p_taskRowStyle_v2, 0) == 0);

        removeForbiddenPreferences(screen, r);
    }

    public static void removeForbiddenPreferences(PreferenceScreen screen, Resources r) {
        int[] forbiddenPrefs = Constants.MARKET_STRATEGY.excludedSettings();
        if (forbiddenPrefs == null) {
            return;
        }
        for (int i : forbiddenPrefs) {
            searchForAndRemovePreference(screen, r.getString(i));
        }
    }

    private static boolean searchForAndRemovePreference(PreferenceGroup group, String key) {
        int preferenceCount = group.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            final Preference preference = group.getPreference(i);
            final String curKey = preference.getKey();

            if (curKey != null && curKey.equals(key)) {
                group.removePreference(preference);
                return true;
            }

            if (preference instanceof PreferenceGroup) {
                if (searchForAndRemovePreference((PreferenceGroup) preference, key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void showBeastMode() {
        Intent intent = new Intent(this, BeastModePreferences.class);
        intent.setAction(AstridApiConstants.ACTION_SETTINGS);
        startActivity(intent);
    }

    private static final HashMap<Class<?>, Integer> PREFERENCE_REQUEST_CODES = new HashMap<>();
    static {
        PREFERENCE_REQUEST_CODES.put(SyncProviderPreferences.class, REQUEST_CODE_SYNC);
    }

    private void addPluginPreferences(PreferenceScreen screen) {
        Intent queryIntent = new Intent(AstridApiConstants.ACTION_SETTINGS);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(queryIntent,
                PackageManager.GET_META_DATA);
        LinkedHashMap<String, ArrayList<Preference>> categoryPreferences =
            new LinkedHashMap<>();

        // Loop through a list of all packages (including plugins, addons)
        // that have a settings action
        for (ResolveInfo resolveInfo : resolveInfoList) {
            final Intent intent = new Intent(AstridApiConstants.ACTION_SETTINGS);
            intent.setClassName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);

            if (GtasksPreferences.class.getName().equals(resolveInfo.activityInfo.name)
                    && AmazonMarketStrategy.isKindleFire()) {
                continue;
            }

            Preference preference = new Preference(this);
            preference.setTitle(resolveInfo.activityInfo.loadLabel(pm));
            Bundle metadata = resolveInfo.activityInfo.metaData;
            if (metadata != null) {
                int resource = metadata.getInt("summary", 0); //$NON-NLS-1$
                if (resource > 0) {
                    preference.setSummary(resource);
                }
            }
            try {
                Class<?> intentComponent = Class.forName(intent.getComponent().getClassName());
                if (intentComponent.getSuperclass().equals(SyncProviderPreferences.class)) {
                    intentComponent = SyncProviderPreferences.class;
                }
                if (PREFERENCE_REQUEST_CODES.containsKey(intentComponent)) {
                    final int code = PREFERENCE_REQUEST_CODES.get(intentComponent);
                    preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference pref) {
                            startActivityForResult(intent, code);
                            return true;
                        }
                    });
                } else {
                    preference.setIntent(intent);
                }
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
                preference.setIntent(intent);
            }

            String category = MetadataHelper.resolveActivityCategoryName(resolveInfo, pm);

            if (!categoryPreferences.containsKey(category)) {
                categoryPreferences.put(category, new ArrayList<Preference>());
            }
            ArrayList<Preference> arrayList = categoryPreferences.get(category);
            arrayList.add(preference);
        }

        for(Entry<String, ArrayList<Preference>> entry : categoryPreferences.entrySet()) {
            if (entry.getKey().equals(getString(R.string.app_name))) {
                for(Preference preference : entry.getValue()) {
                    screen.addPreference(preference);
                }
            } else {
                PreferenceManager manager = getPreferenceManager();
                PreferenceScreen header = manager.createPreferenceScreen(this);
                header.setTitle(entry.getKey());
                if (entry.getKey().equals(getString(R.string.SyP_label))) {
                    header.setSummary(R.string.SyP_summary);
                }
                screen.addPreference(header);

                for(Preference preference : entry.getValue()) {
                    header.addPreference(preference);
                }
            }


        }
    }

    @Override
    public void updatePreferences(final Preference preference, Object value) {
        final Resources r = getResources();

        if (r.getString(R.string.p_taskRowStyle_v2).equals(preference.getKey())) {
            try {
                Integer valueInt = Integer.parseInt((String) value);
                String[] titles = getResources().getStringArray(R.array.EPr_task_row_styles);
                String[] descriptions = getResources().getStringArray(R.array.EPr_task_row_style_descriptions);

                preference.setTitle(getString(R.string.EPr_task_row_style_title, titles[valueInt]));
                preference.setSummary(descriptions[valueInt]);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            preference.setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED) {
                @Override
                public boolean onPreferenceChange(Preference p, Object newValue) {
                    Preference notes = findPreference(getString(R.string.p_showNotes));
                    Preference fullTitle = findPreference(getString(R.string.p_fullTaskTitle));
                    try {
                        int newValueInt = Integer.parseInt((String) newValue);
                        fullTitle.setEnabled(newValueInt != 2);
                        notes.setEnabled(newValueInt == 0);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                    return super.onPreferenceChange(p, newValue);
                }

            });

        } else if (r.getString(R.string.p_showNotes).equals(preference.getKey())) {
            if (value != null && !(Boolean) value) {
                preference.setSummary(R.string.EPr_showNotes_desc_disabled);
            } else {
                preference.setSummary(R.string.EPr_showNotes_desc_enabled);
            }
            if ((Boolean) value != preferences.getBoolean(preference.getKey(), false)) {
                taskService.clearDetails(Criterion.all);
                Flags.set(Flags.REFRESH);
            }
        } else if (r.getString(R.string.p_fullTaskTitle).equals(preference.getKey())) {
            if (value != null && (Boolean) value) {
                preference.setSummary(R.string.EPr_fullTask_desc_enabled);
            } else {
                preference.setSummary(R.string.EPr_fullTask_desc_disabled);
            }
        }

        // pp preferences
        else if (r.getString(R.string.p_files_dir).equals(preference.getKey())) {
            String dir = preferences.getStringValue(TaskAttachment.FILES_DIRECTORY_PREF);

            if (TextUtils.isEmpty(dir)) {
                dir = r.getString(R.string.p_files_dir_desc_default);
            }
            preference.setSummary(r.getString(R.string.p_files_dir_desc, dir));
        } else if (booleanPreference(preference, value, R.string.p_field_missed_calls,
                R.string.MCA_missed_calls_pref_desc_disabled, R.string.MCA_missed_calls_pref_desc_enabled)) {
        } else if (booleanPreference(preference, value, R.string.p_calendar_reminders,
                R.string.CRA_calendar_reminders_pref_desc_disabled, R.string.CRA_calendar_reminders_pref_desc_enabled)) {
        } else if (booleanPreference(preference, value, R.string.p_end_at_deadline,
                R.string.EPr_cal_end_at_due_time, R.string.EPr_cal_start_at_due_time)) {
        } else if (r.getString(R.string.p_voiceInputEnabled).equals(preference.getKey())) {
            if (value != null && !(Boolean) value) {
                preference.setSummary(R.string.EPr_voiceInputEnabled_desc_disabled);
            } else {
                preference.setSummary(R.string.EPr_voiceInputEnabled_desc_enabled);
            }
            onVoiceInputStatusChanged(preference, (Boolean) value);
        } else if (r.getString(R.string.p_voiceRemindersEnabled).equals(preference.getKey())) {
            if (value != null && !(Boolean) value) {
                preference.setSummary(R.string.EPr_voiceRemindersEnabled_desc_disabled);
            } else {
                preference.setSummary(R.string.EPr_voiceRemindersEnabled_desc_enabled);
            }
            onVoiceReminderStatusChanged(preference, (Boolean) value);
        }
    }

    protected boolean booleanPreference(Preference preference, Object value,
            int key, int disabledString, int enabledString) {
        if(getString(key).equals(preference.getKey())) {
            if (value != null && !(Boolean)value) {
                preference.setSummary(disabledString);
            } else {
                preference.setSummary(enabledString);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        voiceOutputAssistant.shutdown();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SYNC && resultCode == SyncProviderPreferences.RESULT_CODE_SYNCHRONIZE) {
            setResult(SyncProviderPreferences.RESULT_CODE_SYNCHRONIZE);
            finish();
            return;
        } else if (requestCode == REQUEST_CODE_FILES_DIR && resultCode == RESULT_OK) {
            if (data != null) {
                String dir = data.getStringExtra(FileExplore.RESULT_DIR_SELECTED);
                preferences.setString(TaskAttachment.FILES_DIRECTORY_PREF, dir);
            }
            return;
        }
        try {
            if (requestCode == REQUEST_CODE_TTS_CHECK) {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    // success, create the TTS instance
                    voiceOutputAssistant.initTTS();
                } else {
                    // missing data, install it
                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installIntent);
                }
            }
        } catch (VerifyError e) {
            // unavailable
            log.error(e.getMessage(), e);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void addPreferenceListeners() {
        findPreference(getString(R.string.p_use_dark_theme)).setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED));

        findPreference(getString(R.string.p_fontSize)).setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED));

        findPreference(getString(R.string.p_hide_plus_button)).setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED));

        findPreference(getString(R.string.p_use_dark_theme_widget)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                WidgetHelper.triggerUpdate(EditPreferences.this);
                updatePreferences(preference, newValue);
                return true;
            }
        });

        if (AndroidUtilities.getSdkVersion() <= 7) {
            searchForAndRemovePreference(getPreferenceScreen(), getString(R.string.p_calendar_reminders));
        } else {
            findPreference(getString(R.string.p_calendar_reminders)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue != null && ((Boolean) newValue)) {
                        calendarAlarmScheduler.scheduleCalendarAlarms(EditPreferences.this, true);
                    }
                    return true;
                }
            });
        }

        findPreference(getString(R.string.p_showNotes)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updatePreferences(preference, newValue);
                return true;
            }
        });

        findPreference(getString(R.string.p_fullTaskTitle)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updatePreferences(preference, newValue);
                return true;
            }
        });
    }

    private void onVoiceReminderStatusChanged(final Preference preference, boolean enabled) {
        try {
            if(enabled && !voiceOutputAssistant.isTTSInitialized()) {
                Intent checkIntent = new Intent();
                checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                startActivityForResult(checkIntent, REQUEST_CODE_TTS_CHECK);
            } else if (!enabled && voiceOutputAssistant.isTTSInitialized()) {
                voiceOutputAssistant.shutdown();
            }
        } catch (VerifyError e) {
            log.error(e.getMessage(), e);
            preference.setEnabled(false);
            preferences.setBoolean(preference.getKey(), false);
        }
    }

    private void onVoiceInputStatusChanged(final Preference preference, boolean newValue) {
        if(!newValue) {
            return;
        }
        int[] excludedSettings = Constants.MARKET_STRATEGY.excludedSettings();
        if (excludedSettings != null && AndroidUtilities.indexOf(excludedSettings, R.string.p_voiceInputEnabled) >= 0) {
            return;
        }

        final Resources r = getResources();
        if (!VoiceRecognizer.voiceInputAvailable(this)) {
            if (AndroidUtilities.getSdkVersion() > 6) {
                DialogUtilities.okCancelDialog(this,
                        r.getString(R.string.EPr_voiceInputInstall_dlg),
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                voiceInputAssistant.showVoiceInputMarketSearch(new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog1,
                                            int which1) {
                                        ((CheckBoxPreference)preference).setChecked(false);
                                    }
                                });
                            }
                        },
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                ((CheckBoxPreference)preference).setChecked(false);
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
                            }
                        });
            }
        }
    }
}
