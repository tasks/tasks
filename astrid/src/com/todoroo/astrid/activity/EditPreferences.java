/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.Toast;

import com.crittercism.app.Crittercism;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.andlib.utility.TodorooPreferenceActivity;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.actfm.ActFmPreferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.FileExplore;
import com.todoroo.astrid.gcal.CalendarStartupReceiver;
import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.helper.MetadataHelper;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.MarketStrategy.AmazonMarketStrategy;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.ui.ContactListAdapter;
import com.todoroo.astrid.ui.TaskListFragmentPager;
import com.todoroo.astrid.utility.AstridDefaultPreferenceSpec;
import com.todoroo.astrid.utility.AstridLitePreferenceSpec;
import com.todoroo.astrid.utility.AstridPreferenceSpec;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.voice.VoiceInputAssistant;
import com.todoroo.astrid.voice.VoiceOutputService;
import com.todoroo.astrid.voice.VoiceRecognizer;
import com.todoroo.astrid.welcome.tutorial.WelcomeWalkthrough;
import com.todoroo.astrid.widget.TasksWidget;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditPreferences extends TodorooPreferenceActivity {

    private static final String SUPPORT_URL = "http://astrid.helpshift.com/a/astrid/?p=android"; //$NON-NLS-1$

    private static final int APPEARANCE_PREFERENCE = 4;

    private static final int REQUEST_CODE_SYNC = 0;
    private static final int REQUEST_CODE_FILES_DIR = 2;

    public static final int RESULT_CODE_THEME_CHANGED = 1;
    public static final int RESULT_CODE_PERFORMANCE_PREF_CHANGED = 3;

    // --- instance variables

    @Autowired private TaskService taskService;
    @Autowired private AddOnService addOnService;
    @Autowired private ActFmPreferenceService actFmPreferenceService;

    @Autowired
    private Database database;

    private VoiceInputAssistant voiceInputAssistant;

    public EditPreferences() {
        DependencyInjectionService.getInstance().inject(this);
    }

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

    private class SetDefaultsClickListener implements OnPreferenceClickListener {
        private final AstridPreferenceSpec spec;
        private final int nameId;
        private final String statistic;
        public SetDefaultsClickListener(AstridPreferenceSpec spec, int nameId, String statistic) {
            this.spec = spec;
            this.nameId = nameId;
            this.statistic = statistic;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            DialogUtilities.okCancelDialog(EditPreferences.this, getString(R.string.EPr_config_dialog_title),
                    getString(R.string.EPr_config_dialog_text, getString(nameId)), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            spec.resetDefaults();
                            StatisticsService.reportEvent(statistic);
                            setResult(RESULT_CODE_PERFORMANCE_PREF_CHANGED);
                            finish();
                        }
                    }, null);
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
        new StartupService().onStartupApplication(this);
        ContextManager.setContext(this);

        PreferenceScreen screen = getPreferenceScreen();
        voiceInputAssistant = new VoiceInputAssistant(this);

        addPluginPreferences(screen);

        addPreferencesFromResource(R.xml.preferences_misc);

        final Resources r = getResources();

        // first-order preferences
        Preference preference = screen.findPreference(getString(R.string.p_about));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showAbout();
                return true;
            }
        });

        preference = screen.findPreference(getString(R.string.p_tutorial));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                Intent showWelcomeLogin = new Intent(EditPreferences.this, WelcomeWalkthrough.class);
                showWelcomeLogin.putExtra(ActFmLoginActivity.SHOW_TOAST, false);
                showWelcomeLogin.putExtra(WelcomeWalkthrough.TOKEN_MANUAL_SHOW, true);
                startActivity(showWelcomeLogin);
                return true;
            }
        });

        preference = screen.findPreference(getString(R.string.p_help));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                showSupport();
                return true;
            }
        });

        preference = screen.findPreference(getString(R.string.p_account));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                showAccountPrefs();
                return true;
            }
        });

        preference = screen.findPreference(getString(R.string.EPr_share_astrid));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                showShareActivity();
                return true;
            }
        });

        Preference beastMode = findPreference(getString(R.string.p_beastMode));
        beastMode.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                showBeastMode();
                return true;
            }
        });

        preference = screen.findPreference(getString(R.string.p_files_dir));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                Intent filesDir = new Intent(EditPreferences.this, FileExplore.class);
                filesDir.putExtra(FileExplore.EXTRA_DIRECTORIES_SELECTABLE, true);
                startActivityForResult(filesDir, REQUEST_CODE_FILES_DIR);
                return true;
            }
        });

        addDebugPreferences();

        addPreferenceListeners();

        disablePremiumPrefs();

        PreferenceScreen appearance = (PreferenceScreen) screen.getPreference(APPEARANCE_PREFERENCE);
        if (!AndroidUtilities.isTabletSized(this)) {
            appearance.removePreference(screen.findPreference(getString(R.string.p_force_phone_layout)));
        } else {
            preference = screen.findPreference(getString(R.string.p_swipe_lists_enabled));
            preference.setEnabled(Preferences.getBoolean(R.string.p_force_phone_layout, false));
        }

        preference = screen.findPreference(getString(R.string.p_showNotes));
        preference.setEnabled(Preferences.getIntegerFromString(R.string.p_taskRowStyle_v2, 0) == 0);

        removeForbiddenPreferences(screen, r);
    }

    public static void removeForbiddenPreferences(PreferenceScreen screen, Resources r) {
        int[] forbiddenPrefs = Constants.MARKET_STRATEGY.excludedSettings();
        if (forbiddenPrefs == null)
            return;
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

    private void disablePremiumPrefs() {
        boolean hasPowerPack = addOnService.hasPowerPack();
        findPreference(getString(R.string.p_files_dir)).setEnabled(ActFmPreferenceService.isPremiumUser());
        findPreference(getString(R.string.p_voiceRemindersEnabled)).setEnabled(hasPowerPack);
        findPreference(getString(R.string.p_statistics)).setEnabled(hasPowerPack);
    }

    /** Show about dialog */
    private void showAbout () {
        String version = "unknown"; //$NON-NLS-1$
        try {
            version = getPackageManager().getPackageInfo(Constants.PACKAGE, 0).versionName;
        } catch (NameNotFoundException e) {
            // sadness
        }
        About.showAbout(this, version);
    }

    private void showSupport() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL));
        startActivity(intent);
    }

    private void showBeastMode() {
        Intent intent = new Intent(this, BeastModePreferences.class);
        intent.setAction(AstridApiConstants.ACTION_SETTINGS);
        startActivity(intent);
    }

    private void showAccountPrefs() {
        if (actFmPreferenceService.isLoggedIn()) {
            Intent intent = new Intent(this, ActFmPreferences.class);
            intent.setAction(AstridApiConstants.ACTION_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_SYNC);
        } else {
            Intent intent = new Intent(this, ActFmLoginActivity.class);
            startActivity(intent);
        }
    }

    private void showShareActivity() {
        Intent intent = new Intent(this, ShareActivity.class);
        startActivity(intent);
    }

    private static final HashMap<Class<?>, Integer> PREFERENCE_REQUEST_CODES = new HashMap<Class<?>, Integer>();
    static {
        PREFERENCE_REQUEST_CODES.put(SyncProviderPreferences.class, REQUEST_CODE_SYNC);
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
            final Intent intent = new Intent(AstridApiConstants.ACTION_SETTINGS);
            intent.setClassName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);

            if (GtasksPreferences.class.getName().equals(resolveInfo.activityInfo.name)
                    && AmazonMarketStrategy.isKindleFire())
                continue;

            Preference preference = new Preference(this);
            preference.setTitle(resolveInfo.activityInfo.loadLabel(pm));
            Bundle metadata = resolveInfo.activityInfo.metaData;
            if (metadata != null) {
                int resource = metadata.getInt("summary", 0); //$NON-NLS-1$
                if (resource > 0)
                    preference.setSummary(resource);
            }
            try {
                Class<?> intentComponent = Class.forName(intent.getComponent().getClassName());
                if (intentComponent.getSuperclass().equals(SyncProviderPreferences.class))
                    intentComponent = SyncProviderPreferences.class;
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
                preference.setIntent(intent);
            }

            String category = MetadataHelper.resolveActivityCategoryName(resolveInfo, pm);

            if(!categoryPreferences.containsKey(category))
                categoryPreferences.put(category, new ArrayList<Preference>());
            ArrayList<Preference> arrayList = categoryPreferences.get(category);
            arrayList.add(preference);
        }

        for(Entry<String, ArrayList<Preference>> entry : categoryPreferences.entrySet()) {
            if (entry.getKey().equals(getString(R.string.app_name))) {
                for(Preference preference : entry.getValue())
                    screen.addPreference(preference);
            } else {
                PreferenceManager manager = getPreferenceManager();
                PreferenceScreen header = manager.createPreferenceScreen(this);
                header.setTitle(entry.getKey());
                if (entry.getKey().equals(getString(R.string.SyP_label)))
                    header.setSummary(R.string.SyP_summary);
                screen.addPreference(header);

                for(Preference preference : entry.getValue())
                    header.addPreference(preference);
            }


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

        preference = new Preference(this);
        preference.setTitle("Make lots of contacts");
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                ContactListAdapter.makeLotsOfContacts();
                DialogUtilities.okDialog(EditPreferences.this, "done", null);
                return false;
            }
        });
        group.addPreference(preference);
    }

    @Override
    public void updatePreferences(final Preference preference, Object value) {
        final Resources r = getResources();

        if (r.getString(R.string.p_account).equals(preference.getKey())) {
            int title;
            int summary;
            if (!actFmPreferenceService.isLoggedIn()) {
                title = R.string.account_type_title_not_logged_in;
                summary = R.string.account_type_summary_not_logged_in;
            } else {
                title = R.string.actfm_account_info;
                summary = R.string.actfm_account_info_summary;
            }
            preference.setTitle(title);
            preference.setSummary(summary);
        } else if (r.getString(R.string.p_taskRowStyle_v2).equals(preference.getKey())) {
            try {
                Integer valueInt = Integer.parseInt((String) value);
                String[] titles = getResources().getStringArray(R.array.EPr_task_row_styles);
                String[] descriptions = getResources().getStringArray(R.array.EPr_task_row_style_descriptions);

                preference.setTitle(getString(R.string.EPr_task_row_style_title, titles[valueInt]));
                preference.setSummary(descriptions[valueInt]);
            } catch (Exception e) {
                //
            }

            preference.setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED) {
                @Override
                public boolean onPreferenceChange(Preference p, Object newValue) {
                    String valueString = newValue.toString();
                    StatisticsService.reportEvent(StatisticsConstants.PREF_CHANGED_PREFIX + "row-style", //$NON-NLS-1$
                            "changed-to", valueString); //$NON-NLS-1$
                    Preference notes = findPreference(getString(R.string.p_showNotes));
                    Preference fullTitle = findPreference(getString(R.string.p_fullTaskTitle));
                    try {
                        int newValueInt = Integer.parseInt((String) newValue);
                        fullTitle.setEnabled(newValueInt != 2);
                        notes.setEnabled(newValueInt == 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return super.onPreferenceChange(p, newValue);
                };
            });

        } else if (r.getString(R.string.p_showNotes).equals(preference.getKey())) {
            if (value != null && !(Boolean)value)
                preference.setSummary(R.string.EPr_showNotes_desc_disabled);
            else
                preference.setSummary(R.string.EPr_showNotes_desc_enabled);
            if((Boolean)value != Preferences.getBoolean(preference.getKey(), false)) {
                taskService.clearDetails(Criterion.all);
                Flags.set(Flags.REFRESH);
            }
        } else if(r.getString(R.string.p_fullTaskTitle).equals(preference.getKey())) {
            if (value != null && (Boolean) value)
                preference.setSummary(R.string.EPr_fullTask_desc_enabled);
            else
                preference.setSummary(R.string.EPr_fullTask_desc_disabled);
        } else if (r.getString(R.string.p_theme).equals(preference.getKey())) {
            if(AndroidUtilities.getSdkVersion() < 5) {
                preference.setEnabled(false);
                preference.setSummary(R.string.EPr_theme_desc_unsupported);
            } else {
                int index = 0;
                if(value instanceof String && !TextUtils.isEmpty((String)value))
                    index = AndroidUtilities.indexOf(r.getStringArray(R.array.EPr_theme_settings), (String)value);
                if (index < 0)
                    index = 0;
                preference.setSummary(getString(R.string.EPr_theme_desc,
                        r.getStringArray(R.array.EPr_themes)[index]));
            }
        } else if (r.getString(R.string.p_theme_widget).equals(preference.getKey())) {
            if(AndroidUtilities.getSdkVersion() < 5) {
                preference.setEnabled(false);
                preference.setSummary(R.string.EPr_theme_desc_unsupported);
            } else {
                int index = 0;
                if(value instanceof String && !TextUtils.isEmpty((String)value))
                    index = AndroidUtilities.indexOf(r.getStringArray(R.array.EPr_theme_widget_settings), (String)value);
                if (index < 0)
                    index = 0;
                preference.setSummary(getString(R.string.EPr_theme_desc,
                        r.getStringArray(R.array.EPr_themes_widget)[index]));
            }
        }

        // pp preferences
        else if (r.getString(R.string.p_files_dir).equals(preference.getKey())) {
            String dir = Preferences.getStringValue(TaskAttachment.FILES_DIRECTORY_PREF);

            if (TextUtils.isEmpty(dir)) {
                dir = r.getString(R.string.p_files_dir_desc_default);
            }
            preference.setSummary(r.getString(R.string.p_files_dir_desc, dir));
        }
        else if (booleanPreference(preference, value, R.string.p_statistics,
                R.string.EPr_statistics_desc_disabled, R.string.EPr_statistics_desc_enabled));
        else if (booleanPreference(preference, value, R.string.p_field_missed_calls,
                    R.string.MCA_missed_calls_pref_desc_disabled, R.string.MCA_missed_calls_pref_desc_enabled));
        else if (booleanPreference(preference, value, R.string.p_calendar_reminders,
                    R.string.CRA_calendar_reminders_pref_desc_disabled, R.string.CRA_calendar_reminders_pref_desc_enabled));
        else if (booleanPreference(preference, value, R.string.p_use_contact_picker,
                    R.string.EPr_use_contact_picker_desc_disabled, R.string.EPr_use_contact_picker_desc_enabled));
        else if (booleanPreference(preference, value, R.string.p_end_at_deadline,
                    R.string.EPr_cal_end_at_due_time, R.string.EPr_cal_start_at_due_time));
        else if (r.getString(R.string.p_swipe_lists_enabled).equals(preference.getKey())) {
            preference.setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED) {
                @Override
                public boolean onPreferenceChange(Preference p, Object newValue) {
                    // If the user changes the setting themselves, no need to show the helper
                    Preferences.setBoolean(TaskListFragmentPager.PREF_SHOWED_SWIPE_HELPER, true);
                    return super.onPreferenceChange(p, newValue);
                }
            });
        }
        else if (r.getString(R.string.p_force_phone_layout).equals(preference.getKey())) {
             preference.setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED) {
                 @Override
                public boolean onPreferenceChange(Preference p, Object newValue) {
                    Preference swipe = findPreference(getString(R.string.p_swipe_lists_enabled));
                    swipe.setEnabled((Boolean) newValue);
                    return super.onPreferenceChange(p, newValue);
                }
             });
        }
        else if (r.getString(R.string.p_voiceInputEnabled).equals(preference.getKey())) {
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
        }
    }

    protected boolean booleanPreference(Preference preference, Object value,
            int key, int disabledString, int enabledString) {
        if(getString(key).equals(preference.getKey())) {
            if (value != null && !(Boolean)value)
                preference.setSummary(disabledString);
            else
                preference.setSummary(enabledString);
            return true;
        }
        return false;
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
                Preferences.setString(TaskAttachment.FILES_DIRECTORY_PREF, dir);
            }
            return;
        }
        try {
            VoiceOutputService.getVoiceOutputInstance().handleActivityResult(requestCode, resultCode, data);
        } catch (VerifyError e) {
            // unavailable
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void addPreferenceListeners() {
        findPreference(getString(R.string.p_theme)).setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED));

        findPreference(getString(R.string.p_fontSize)).setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED));

        findPreference(getString(R.string.p_hide_plus_button)).setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED));

        findPreference(getString(R.string.p_config_default)).setOnPreferenceClickListener(
                new SetDefaultsClickListener(new AstridDefaultPreferenceSpec(), R.string.EPr_config_dialog_default_id, StatisticsConstants.PREFS_RESET_DEFAULT));

        findPreference(getString(R.string.p_config_lite)).setOnPreferenceClickListener(
                new SetDefaultsClickListener(new AstridLitePreferenceSpec(), R.string.EPr_config_lite, StatisticsConstants.PREFS_RESET_LITE));

        int[] menuPrefs = { R.string.p_show_menu_search, R.string.p_show_menu_friends,
                R.string.p_show_menu_sync, R.string.p_show_menu_sort,
        };
        for (int key : menuPrefs) {
            findPreference(getString(key)).setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED));
        }

        findPreference(getString(R.string.p_theme_widget)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                TasksWidget.updateWidgets(EditPreferences.this);
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
                    if (newValue != null && ((Boolean) newValue))
                        CalendarStartupReceiver.scheduleCalendarAlarms(EditPreferences.this, true);
                    return true;
                }
            });
        }

        findPreference(getString(R.string.p_statistics)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Boolean value = (Boolean) newValue;
                try {
                    if (!value.booleanValue()) {
                        Crittercism.setOptOutStatus(true);
                    } else {
                        Crittercism.setOptOutStatus(false);
                    }
                } catch (NullPointerException e) {
                    return false;
                }
                return true;
            }
        });

        findPreference(getString(R.string.p_showNotes)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updatePreferences(preference, newValue);
                StatisticsService.reportEvent(StatisticsConstants.PREF_SHOW_NOTES_IN_ROW, "enabled", newValue.toString()); //$NON-NLS-1$
                return true;
            }
        });

        findPreference(getString(R.string.p_fullTaskTitle)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updatePreferences(preference, newValue);
                StatisticsService.reportEvent(StatisticsConstants.PREF_CHANGED_PREFIX + "full-title", "full-title", newValue.toString()); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            }
        });
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
        int[] excludedSettings = Constants.MARKET_STRATEGY.excludedSettings();
        if (excludedSettings != null && AndroidUtilities.indexOf(excludedSettings, R.string.p_voiceInputEnabled) >= 0)
            return;

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

    @Override
    protected void onPause() {
        StatisticsService.sessionPause();
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatisticsService.sessionStart(this);
    }

    @Override
    protected void onStop() {
        StatisticsService.sessionStop(this);
        super.onStop();
    }

}
