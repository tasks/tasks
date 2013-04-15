package com.timsu.astrid;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMConstants;
import com.google.android.gcm.GCMRegistrar;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread;
import com.todoroo.astrid.actfm.sync.ActFmSyncV2Provider;
import com.todoroo.astrid.actfm.sync.messages.BriefMe;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.sync.SyncResultCallbackAdapter;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class GCMIntentService extends GCMBaseIntentService {

    public static final String SENDER_ID = "1003855277730"; //$NON-NLS-1$
    public static final String PREF_REGISTRATION = "gcm_id";
    public static final String PREF_NEEDS_REGISTRATION = "gcm_needs_reg";
    public static final String PREF_NEEDS_RETRY = "gcm_needs_retry";

    private static final String PREF_LAST_GCM = "c2dm_last";
    public static final String PREF_C2DM_REGISTRATION = "c2dm_key";

    public static String getDeviceID() {
        String id = Secure.getString(ContextManager.getContext().getContentResolver(), Secure.ANDROID_ID);;
        if(AndroidUtilities.getSdkVersion() > 8) { //Gingerbread and above
            //the following uses relection to get android.os.Build.SERIAL to avoid having to build with Gingerbread
            try {
                if(!Build.UNKNOWN.equals(Build.SERIAL))
                    id = Build.SERIAL;
            } catch(Exception e) {
                // Ah well
            }
        }

        if (TextUtils.isEmpty(id) || "9774d56d682e549c".equals(id)) { // check for failure or devices affected by the "9774d56d682e549c" bug
            return null;
        }

        return id;
    }

    static {
        AstridDependencyInjector.initialize();
    }

    @Autowired
    private ActFmSyncService actFmSyncService;

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TagDataService tagDataService;

    @Autowired
    private UserActivityDao userActivityDao;

    public GCMIntentService() {
        super(SENDER_ID);
        DependencyInjectionService.getInstance().inject(this);
    }


    // ===================== Messaging =================== //

    private final SyncResultCallbackAdapter refreshOnlyCallback = new SyncResultCallbackAdapter() {
        @Override
        public void finished() {
            ContextManager.getContext().sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
        }
    };

    private static final long MIN_MILLIS_BETWEEN_FULL_SYNCS = DateUtilities.ONE_HOUR;

    @Override
    protected void onMessage(Context context, Intent intent) {
        if (actFmPreferenceService.isLoggedIn()) {
            if(intent.hasExtra("web_update"))
                if (DateUtilities.now() - actFmPreferenceService.getLastSyncDate() > MIN_MILLIS_BETWEEN_FULL_SYNCS && !actFmPreferenceService.isOngoing())
                    new ActFmSyncV2Provider().synchronizeActiveTasks(false, refreshOnlyCallback);
                else
                    handleWebUpdate(intent);
            else
                handleMessage(intent);
        }
    }

    /** Handle web task or list changed */
    protected void handleWebUpdate(Intent intent) {
        if(intent.hasExtra("tag_id")) {
            String uuid = intent.getStringExtra("tag_id");
            TodorooCursor<TagData> cursor = tagDataService.query(
                    Query.select(TagData.PUSHED_AT).where(TagData.UUID.eq(
                            uuid)));
            long pushedAt = 0;
            try {
                TagData tagData = new TagData();
                if(cursor.getCount() > 0) {
                    cursor.moveToNext();
                    tagData.readFromCursor(cursor);
                    pushedAt = tagData.getValue(TagData.PUSHED_AT);
                }
            } finally {
                cursor.close();
            }
            ActFmSyncThread.getInstance().enqueueMessage(new BriefMe<TagData>(TagData.class, uuid, pushedAt), ActFmSyncThread.DEFAULT_REFRESH_RUNNABLE);
        } else if(intent.hasExtra("task_id")) {
            String uuid = intent.getStringExtra("task_id");
            TodorooCursor<Task> cursor = taskService.query(
                    Query.select(Task.PROPERTIES).where(Task.UUID.eq(
                            uuid)));
            long pushedAt = 0;
            try {
                final Task task = new Task();
                if(cursor.getCount() > 0) {
                    cursor.moveToNext();
                    task.readFromCursor(cursor);
                    pushedAt = task.getValue(Task.PUSHED_AT);
                }
                ActFmSyncThread.getInstance().enqueueMessage(new BriefMe<Task>(Task.class, uuid, pushedAt), ActFmSyncThread.DEFAULT_REFRESH_RUNNABLE);
            } finally {
                cursor.close();
            }
        }
    }

    // --- message handling

    /** Handle message. Run on separate thread. */
    private void handleMessage(Intent intent) {
        String message = intent.getStringExtra("alert");
        Context context = ContextManager.getContext();
        if(TextUtils.isEmpty(message))
            return;

        long lastNotification = Preferences.getLong(PREF_LAST_GCM, 0);
        if(DateUtilities.now() - lastNotification < 5000L)
            return;
        Preferences.setLong(PREF_LAST_GCM, DateUtilities.now());
        Intent notifyIntent = null;
        int notifId;

        final String user_id = intent.getStringExtra("oid");
        final String token_id = intent.getStringExtra("tid");
        // unregister
        if (!actFmPreferenceService.isLoggedIn() || !ActFmPreferenceService.userId().equals(user_id)) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        actFmSyncService.invoke("user_unset_c2dm", "tid", token_id, "oid", user_id);
                    } catch (IOException e) {
                        //
                    }
                }
            }.start();
            return;
        }


        // fetch data
        if(intent.hasExtra("tag_id")) {
            notifyIntent = createTagIntent(context, intent);
            notifId = intent.getStringExtra("tag_id").hashCode();
        } else if(intent.hasExtra("task_id")) {
            notifyIntent = createTaskIntent(intent);
            notifId = intent.getStringExtra("task_id").hashCode();
        } else {
            return;
        }

        if (notifyIntent == null)
            return;

        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notifyIntent.putExtra(TaskListActivity.TOKEN_SOURCE, Constants.SOURCE_C2DM);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                notifId, notifyIntent, 0);

        int icon = calculateIcon(intent);

        // create notification
        NotificationManager nm = new AndroidNotificationManager(ContextManager.getContext());
        Notification notification = new Notification(icon,
                message, System.currentTimeMillis());
        String title;
        if(intent.hasExtra("title"))
            title = "Astrid: " + intent.getStringExtra("title");
        else
            title = ContextManager.getString(R.string.app_name);
        notification.setLatestEventInfo(ContextManager.getContext(), title,
                message, pendingIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        boolean sounds = !"false".equals(intent.getStringExtra("sound"));
        notification.defaults = 0;
        if(sounds && !Notifications.isQuietHours()) {
            notification.defaults |= Notification.DEFAULT_SOUND;
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        nm.notify(notifId, notification);

        if(intent.hasExtra("tag_id")) {
            Intent broadcastIntent = new Intent(TagViewFragment.BROADCAST_TAG_ACTIVITY);
            broadcastIntent.putExtras(intent);
            ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        }
    }

    private int calculateIcon(Intent intent) {
        if(intent.hasExtra("type")) {
            String type = intent.getStringExtra("type");
            if("f".equals(type))
                return R.drawable.notif_c2dm_done;
            if("s".equals(type))
                return R.drawable.notif_c2dm_assign;
            if("l".equals(type))
                return R.drawable.notif_c2dm_assign;
        } else {
            String message = intent.getStringExtra("alert");
            if(message.contains(" finished "))
                return R.drawable.notif_c2dm_done;
            if(message.contains(" invited you to "))
                return R.drawable.notif_c2dm_assign;
            if(message.contains(" sent you "))
                return R.drawable.notif_c2dm_assign;
        }
        return R.drawable.notif_c2dm_msg;
    }

    private Intent createTaskIntent(Intent intent) {
        String uuid = intent.getStringExtra("task_id");
        TodorooCursor<Task> cursor = taskService.query(
                Query.select(Task.PROPERTIES).where(Task.UUID.eq(
                        uuid)));
        long pushedAt = 0;
        try {
            final Task task = new Task();
            if(cursor.getCount() == 0) {
                task.setValue(Task.TITLE, intent.getStringExtra("title"));
                task.setValue(Task.UUID, intent.getStringExtra("task_id"));
                task.setValue(Task.USER_ID, Task.USER_ID_UNASSIGNED);
                task.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                taskService.save(task);
            } else {
                cursor.moveToNext();
                task.readFromCursor(cursor);
                pushedAt = task.getValue(Task.PUSHED_AT);
            }
            ActFmSyncThread.getInstance().enqueueMessage(new BriefMe<Task>(Task.class, uuid, pushedAt), null);

            Filter filter = new Filter("", task.getValue(Task.TITLE),
                    new QueryTemplate().where(Task.ID.eq(task.getId())),
                    null);

            Intent launchIntent = ShortcutActivity.createIntent(filter);
            return launchIntent;
        } finally {
            cursor.close();
        }
    }

    private Intent createTagIntent(final Context context, final Intent intent) {
        String uuid = intent.getStringExtra("tag_id");
        TodorooCursor<TagData> cursor = tagDataService.query(
                Query.select(TagData.PROPERTIES).where(TagData.UUID.eq(
                        uuid)));
        long pushedAt = 0;
        try {
            final TagData tagData = new TagData();
            if(cursor.getCount() == 0) {
                tagData.setValue(TagData.NAME, intent.getStringExtra("title"));
                tagData.setValue(TagData.UUID, intent.getStringExtra("tag_id"));
                tagData.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                tagDataService.save(tagData);
            } else {
                cursor.moveToNext();
                tagData.readFromCursor(cursor);
                pushedAt = tagData.getValue(TagData.PUSHED_AT);
            }
            ActFmSyncThread.getInstance().enqueueMessage(new BriefMe<TagData>(TagData.class, uuid, pushedAt), null);

            FilterWithCustomIntent filter = (FilterWithCustomIntent)TagFilterExposer.filterFromTagData(context, tagData);

            if(intent.hasExtra("activity_id")) {
                UserActivity update = new UserActivity();
                update.setValue(UserActivity.UUID, intent.getStringExtra("activity_id"));
                update.setValue(UserActivity.USER_UUID, intent.getStringExtra("user_id"));

                update.setValue(UserActivity.ACTION, "tag_comment");
                update.setValue(UserActivity.TARGET_NAME, intent.getStringExtra("title"));
                String message = intent.getStringExtra("alert");
                if(message.contains(":"))
                    message = message.substring(message.indexOf(':') + 2);
                update.setValue(UserActivity.MESSAGE, message);
                update.setValue(UserActivity.CREATED_AT, DateUtilities.now());
                update.setValue(UserActivity.TARGET_ID, intent.getStringExtra("tag_id"));
                update.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                userActivityDao.createNew(update);
            }

            if (filter != null) {
                Intent launchIntent = new Intent(context, TaskListActivity.class);
                launchIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
                filter.customExtras.putBoolean(TagViewFragment.TOKEN_START_ACTIVITY, shouldLaunchActivity(intent));
                launchIntent.putExtras(filter.customExtras);

                return launchIntent;
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    private boolean shouldLaunchActivity(Intent intent) {
        if(intent.hasExtra("type")) {
            String type = intent.getStringExtra("type");
            if("f".equals(type)) return true;
            if("s".equals(type)) return false;
            if("l".equals(type)) return false;
        } else {
            String message = intent.getStringExtra("alert");
            if(message.contains(" finished ")) return true;
            if(message.contains(" invited you to ")) return false;
            if(message.contains(" sent you ")) return false;
        }
        return true;
    }

    // ==================== Registration ============== //

    public static final void register(Context context) {
        try {
            if (AndroidUtilities.getSdkVersion() >= 8) {
                GCMRegistrar.checkDevice(context);
                GCMRegistrar.checkManifest(context);
                final String regId = GCMRegistrar.getRegistrationId(context);
                if ("".equals(regId)) {
                    GCMRegistrar.register(context, GCMIntentService.SENDER_ID);
                } else {
                    // TODO: Already registered--do something?
                }
            }
        } catch (Exception e) {
            // phone may not support gcm
            Log.e("actfm-sync", "gcm-register", e);
        }
    }

    public static final void unregister(Context context) {
        try {
            if (AndroidUtilities.getSdkVersion() >= 8) {
                GCMRegistrar.unregister(context);
            }
        } catch (Exception e) {
            Log.e("actfm-sync", "gcm-unregister", e);
        }
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        actFmSyncService.setGCMRegistration(registrationId);
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        // Server can unregister automatically next time it tries to send a message
    }


    @Override
    protected void onError(Context context, String intent) {
        if ((GCMConstants.ERROR_AUTHENTICATION_FAILED.equals(intent) || GCMConstants.ERROR_ACCOUNT_MISSING.equals(intent))
                && !Preferences.getBoolean(PREF_NEEDS_RETRY, false)) {
            Preferences.setBoolean(PREF_NEEDS_RETRY, true);
        }
    }

    // =========== Migration ============= //

    public static class GCMMigration {
        @Autowired
        private ActFmPreferenceService actFmPreferenceService;

        public GCMMigration() {
            DependencyInjectionService.getInstance().inject(this);
        }

        public void performMigration(Context context) {
            if (actFmPreferenceService.isLoggedIn()) {
                GCMIntentService.register(context);
            }
        }
    }

}
