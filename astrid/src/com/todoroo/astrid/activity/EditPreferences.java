/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.weloveastrid.rmilk.MilkPreferences;
import org.weloveastrid.rmilk.MilkUtilities;

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
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.Toast;

import com.crittercism.NewFeedbackSpringboardActivity;
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
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.LabsPreferences;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.helper.MetadataHelper;
import com.todoroo.astrid.producteev.ProducteevPreferences;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.MarketStrategy.AmazonMarketStrategy;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.ui.ContactListAdapter;
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

    private static final String SUPPORT_URL = "http://blog.astrid.com/topics/support/android"; //$NON-NLS-1$

    private static final int APPEARANCE_PREFERENCE = 4;
    private static final int POWER_PACK_PREFERENCE = 5;

    private static final int REQUEST_CODE_SYNC = 0;
    private static final int REQUEST_CODE_PERFORMANCE = 1;

    public static final int RESULT_CODE_THEME_CHANGED = 1;
    public static final int RESULT_CODE_PERFORMANCE_PREF_CHANGED = 3;

    // --- instance variables

    @Autowired private TaskService taskService;
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

        preference = screen.findPreference(getString(R.string.p_forums));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                showForums();
                return true;
            }
        });

        PreferenceCategory appearance = (PreferenceCategory) screen.getPreference(APPEARANCE_PREFERENCE);
        Preference beastMode = appearance.getPreference(1);
        beastMode.setTitle(r.getString(R.string.EPr_beastMode_title));
        beastMode.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                showBeastMode();
                return true;
            }
        });

        addDebugPreferences();

        addPreferenceListeners();
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

    private void showForums() {
        StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_HELP);
        Intent intent = new Intent(this, NewFeedbackSpringboardActivity.class);
        startActivity(intent);
    }

    private void showBeastMode() {
        Intent intent = new Intent(this, BeastModePreferences.class);
        intent.setAction(AstridApiConstants.ACTION_SETTINGS);
        startActivity(intent);
    }

    private static final HashMap<Class<?>, Integer> PREFERENCE_REQUEST_CODES = new HashMap<Class<?>, Integer>();
    static {
        PREFERENCE_REQUEST_CODES.put(SyncProviderPreferences.class, REQUEST_CODE_SYNC);
        PREFERENCE_REQUEST_CODES.put(LabsPreferences.class, REQUEST_CODE_PERFORMANCE);
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
        String labsTitle = getString(R.string.EPr_labs_header);
        for(int i = 0; i < length; i++) {
            ResolveInfo resolveInfo = resolveInfoList.get(i);
            final Intent intent = new Intent(AstridApiConstants.ACTION_SETTINGS);
            intent.setClassName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);

            if(MilkPreferences.class.getName().equals(resolveInfo.activityInfo.name) &&
                    !MilkUtilities.INSTANCE.isLoggedIn())
                continue;

            if (GtasksPreferences.class.getName().equals(resolveInfo.activityInfo.name)
                    && AmazonMarketStrategy.isKindleFire())
                continue;

            if (ProducteevPreferences.class.getName().equals(resolveInfo.activityInfo.name)
                    && !Preferences.getBoolean(R.string.p_third_party_addons, false) && !ProducteevUtilities.INSTANCE.isLoggedIn())
                continue;

            Preference preference = new Preference(this);
            preference.setTitle(resolveInfo.activityInfo.loadLabel(pm));
            if (labsTitle.equals(preference.getTitle()))
                preference.setSummary(R.string.EPr_labs_desc);
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

        if (r.getString(R.string.p_showNotes).equals(preference.getKey())) {
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
        else if (booleanPreference(preference, value, R.string.p_statistics,
                R.string.EPr_statistics_desc_disabled, R.string.EPr_statistics_desc_enabled))
            ;
        else if (booleanPreference(preference, value, R.string.p_autoIdea,
                R.string.EPr_ideaAuto_desc_disabled, R.string.EPr_ideaAuto_desc_enabled))
            ;


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
        } else if (requestCode == REQUEST_CODE_PERFORMANCE && resultCode == LabsPreferences.PERFORMANCE_SETTING_CHANGED) {
            setResult(RESULT_CODE_PERFORMANCE_PREF_CHANGED);
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
        findPreference(getString(R.string.p_theme)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setResult(RESULT_CODE_THEME_CHANGED);
                return true;
            }
        });

        findPreference(getString(R.string.p_theme_widget)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                TasksWidget.updateWidgets(EditPreferences.this);
                return true;
            }
        });

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
