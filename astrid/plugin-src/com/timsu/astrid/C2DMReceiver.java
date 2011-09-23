package com.timsu.astrid;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.TagViewActivity;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncProvider;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;

@SuppressWarnings("nls")
public class C2DMReceiver extends BroadcastReceiver {

    public static final String C2DM_SENDER = "c2dm@astrid.com"; //$NON-NLS-1$

    private static final String PREF_REGISTRATION = "c2dm_key";
    private static final String PREF_LAST_C2DM = "c2dm_last";

    private static final long MIN_MILLIS_BETWEEN_FULL_SYNCS = 5 * DateUtilities.ONE_MINUTE;

    @Autowired ActFmSyncService actFmSyncService;
    @Autowired TaskService taskService;
    @Autowired TagDataService tagDataService;
    @Autowired UpdateDao updateDao;
    @Autowired ActFmPreferenceService actFmPreferenceService;

    static {
        AstridDependencyInjector.initialize();
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        ContextManager.setContext(context);
        DependencyInjectionService.getInstance().inject(this);
        if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
            handleRegistration(intent);
        } else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(intent.hasExtra("web_update"))
                        if (DateUtilities.now() - actFmPreferenceService.getLastSyncDate() > MIN_MILLIS_BETWEEN_FULL_SYNCS)
                            new ActFmSyncProvider().synchronize(ContextManager.getContext());
                        else
                            handleWebUpdate(intent);
                    else
                        handleMessage(intent);
                }
            }).start();
         }
     }

    // --- web update handling

    /** Handle web task or list changed */
    protected void handleWebUpdate(Intent intent) {
        try {
            if(intent.hasExtra("tag_id")) {
                TodorooCursor<TagData> cursor = tagDataService.query(
                        Query.select(TagData.PROPERTIES).where(TagData.REMOTE_ID.eq(
                                intent.getStringExtra("tag_id"))));
                try {
                    TagData tagData = new TagData();
                    if(cursor.getCount() == 0) {
                        tagData.setValue(Task.REMOTE_ID, Long.parseLong(intent.getStringExtra("tag_id")));
                        Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
                        tagDataService.save(tagData);
                    } else {
                        cursor.moveToNext();
                        tagData.readFromCursor(cursor);
                    }

                    actFmSyncService.fetchTag(tagData);
                } finally {
                    cursor.close();
                }
            } else if(intent.hasExtra("task_id")) {
                TodorooCursor<Task> cursor = taskService.query(
                        Query.select(Task.PROPERTIES).where(Task.REMOTE_ID.eq(
                                intent.getStringExtra("task_id"))));
                try {
                    final Task task = new Task();
                    if(cursor.getCount() == 0) {
                        task.setValue(Task.REMOTE_ID, Long.parseLong(intent.getStringExtra("task_id")));
                        Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
                        taskService.save(task);
                    } else {
                        cursor.moveToNext();
                        task.readFromCursor(cursor);
                    }

                    actFmSyncService.fetchTask(task);
                } finally {
                    cursor.close();
                }
            }

            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

        } catch (IOException e) {
            Log.e("c2dm-tag-rx", "io-exception", e);
            return;
        } catch (JSONException e) {
            Log.e("c2dm-tag-rx", "json-exception", e);
        }
    }

    // --- message handling

    /** Handle message. Run on separate thread. */
    private void handleMessage(Intent intent) {
        String message = intent.getStringExtra("alert");
        Context context = ContextManager.getContext();
        if(TextUtils.isEmpty(message))
            return;

        long lastNotification = Preferences.getLong(PREF_LAST_C2DM, 0);
        if(DateUtilities.now() - lastNotification < 5000L)
            return;
        Preferences.setLong(PREF_LAST_C2DM, DateUtilities.now());
        Intent notifyIntent = null;
        int notifId;

        // fetch data
        if(intent.hasExtra("tag_id")) {
            notifyIntent = createTagIntent(context, intent);
            notifId = (int) Long.parseLong(intent.getStringExtra("tag_id"));
        } else if(intent.hasExtra("task_id")) {
            notifyIntent = createTaskIntent(intent);
            notifId = (int) Long.parseLong(intent.getStringExtra("task_id"));
        } else {
            return;
        }

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
            Intent broadcastIntent = new Intent(TagViewActivity.BROADCAST_TAG_ACTIVITY);
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
        TodorooCursor<Task> cursor = taskService.query(
                Query.select(Task.PROPERTIES).where(Task.REMOTE_ID.eq(
                        intent.getStringExtra("task_id"))));
        try {
            final Task task = new Task();
            if(cursor.getCount() == 0) {
                task.setValue(Task.TITLE, intent.getStringExtra("title"));
                task.setValue(Task.REMOTE_ID, Long.parseLong(intent.getStringExtra("task_id")));
                task.setValue(Task.USER_ID, -1L); // set it to invalid number because we don't know whose it is
                Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
                taskService.save(task);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            actFmSyncService.fetchTask(task);
                        } catch (IOException e) {
                            Log.e("c2dm-task-rx", "io-exception", e);
                        } catch (JSONException e) {
                            Log.e("c2dm-task-rx", "json-exception", e);
                        }
                    }
                }).start();
            } else {
                cursor.moveToNext();
                task.readFromCursor(cursor);
            }

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
        TodorooCursor<TagData> cursor = tagDataService.query(
                Query.select(TagData.PROPERTIES).where(TagData.REMOTE_ID.eq(
                        intent.getStringExtra("tag_id"))));
        try {
            final TagData tagData = new TagData();
            if(cursor.getCount() == 0) {
                tagData.setValue(TagData.NAME, intent.getStringExtra("title"));
                tagData.setValue(TagData.REMOTE_ID, Long.parseLong(intent.getStringExtra("tag_id")));
                Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
                tagDataService.save(tagData);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            actFmSyncService.fetchTag(tagData);
                        } catch (IOException e) {
                            Log.e("c2dm-tag-rx", "io-exception", e);
                        } catch (JSONException e) {
                            Log.e("c2dm-tag-rx", "json-exception", e);
                        }
                    }
                }).start();
            } else {
                cursor.moveToNext();
                tagData.readFromCursor(cursor);
            }

            FilterWithCustomIntent filter = (FilterWithCustomIntent)TagFilterExposer.filterFromTagData(context, tagData);
            //filter.customExtras.putString(TagViewActivity.EXTRA_START_TAB, "updates");
            if(intent.hasExtra("activity_id")) {
                try {
                    Update update = new Update();
                    update.setValue(Update.REMOTE_ID, Long.parseLong(intent.getStringExtra("activity_id")));
                    update.setValue(Update.USER_ID, Long.parseLong(intent.getStringExtra("user_id")));
                    JSONObject user = new JSONObject();
                    user.put("id", update.getValue(Update.USER_ID));
                    user.put("name", intent.getStringExtra("user_name"));
                    update.setValue(Update.USER, user.toString());
                    update.setValue(Update.ACTION, "commented");
                    update.setValue(Update.ACTION_CODE, "tag_comment");
                    update.setValue(Update.TARGET_NAME, intent.getStringExtra("title"));
                    String message = intent.getStringExtra("alert");
                    if(message.contains(":"))
                        message = message.substring(message.indexOf(':') + 2);
                    update.setValue(Update.MESSAGE, message);
                    update.setValue(Update.CREATION_DATE, DateUtilities.now());
                    update.setValue(Update.TAGS, "," + intent.getStringExtra("tag_id") + ",");
                    updateDao.createNew(update);
                } catch (JSONException e) {
                    //
                } catch (NumberFormatException e) {
                    //
                }
            }

            Intent launchIntent = new Intent();
            launchIntent.putExtra(TaskListActivity.TOKEN_FILTER, filter);
            launchIntent.setComponent(filter.customTaskList);
            launchIntent.putExtras(filter.customExtras);

            return launchIntent;
        } finally {
            cursor.close();
        }
    }

    private void handleRegistration(Intent intent) {
        String registration = intent.getStringExtra("registration_id");
        if (intent.getStringExtra("error") != null) {
            Log.w("astrid-actfm", "error-c2dm: " + intent.getStringExtra("error"));
        } else if (intent.getStringExtra("unregistered") != null) {
            // un-registration done
        } else if (registration != null) {
            try {
                DependencyInjectionService.getInstance().inject(this);
                actFmSyncService.invoke("user_set_c2dm", "c2dm", registration);
                Preferences.setString(PREF_REGISTRATION, registration);
            } catch (IOException e) {
                Log.e("astrid-actfm", "error-c2dm-transfer", e);
            }
        }
    }

    /** try to request registration from c2dm service */
    public static void register() {
        if(Preferences.getStringValue(PREF_REGISTRATION) != null)
            return;

        Context context = ContextManager.getContext();
        Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
        registrationIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0)); // boilerplate
        registrationIntent.putExtra("sender", C2DM_SENDER);
        context.startService(registrationIntent);
    }

    /** unregister with c2dm service */
    public static void unregister() {
        Preferences.setString(PREF_REGISTRATION, null);
        Context context = ContextManager.getContext();
        Intent unregIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
        unregIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        context.startService(unregIntent);
    }

}
