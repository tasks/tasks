/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
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
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.actfm.sync.ActFmSyncV2Provider;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.sync.SyncResultCallbackAdapter;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class C2DMReceiver extends BroadcastReceiver {

    public static final String C2DM_SENDER = "c2dm@astrid.com"; //$NON-NLS-1$

    private static final String PREF_REGISTRATION = "c2dm_key";
    private static final String PREF_LAST_C2DM = "c2dm_last";

    private static final long MIN_MILLIS_BETWEEN_FULL_SYNCS = DateUtilities.ONE_HOUR;

    @Autowired TaskService taskService;
    @Autowired TagDataService tagDataService;
    @Autowired UpdateDao updateDao;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired ActFmSyncService actFmSyncService;

    static {
        AstridDependencyInjector.initialize();
    }

    private final SyncResultCallbackAdapter refreshOnlyCallback = new SyncResultCallbackAdapter() {
        @Override
        public void finished() {
            ContextManager.getContext().sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
        }
    };

    @Override
    public void onReceive(Context context, final Intent intent) {
        if (Preferences.getStringValue(GCMIntentService.PREF_REGISTRATION) != null) // GCM takes precedence
            return;

        ContextManager.setContext(context);
        DependencyInjectionService.getInstance().inject(this);
        if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
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
                        tagData.setValue(TagData.REMOTE_ID, Long.parseLong(intent.getStringExtra("tag_id")));
                        tagData.putTransitory(SyncFlags.ACTFM_SUPPRESS_SYNC, true);
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
                        task.putTransitory(SyncFlags.ACTFM_SUPPRESS_SYNC, true);
                        taskService.save(task);
                    } else {
                        cursor.moveToNext();
                        task.readFromCursor(cursor);
                    }

                    actFmSyncService.fetchTask(task);
                } catch(NumberFormatException e) {
                    // invalid task id
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

        long user_idTemp = -2;
        final String user_idString = intent.getStringExtra("oid");
        if (user_idString != null) {
            try {
                user_idTemp = Long.parseLong(user_idString);
            } catch(NumberFormatException e) {
                // We tried
                Log.e("c2dm-receive", "oid-parse", e);
            }
        }
        final long user_id = user_idTemp;
        final String token_id = intent.getStringExtra("tid");
        // unregister
        if (!actFmPreferenceService.isLoggedIn() || user_id != ActFmPreferenceService.userId()) {

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
        TodorooCursor<Task> cursor = taskService.query(
                Query.select(Task.PROPERTIES).where(Task.REMOTE_ID.eq(
                        intent.getStringExtra("task_id"))));
        try {
            final Task task = new Task();
            if(cursor.getCount() == 0) {
                task.setValue(Task.TITLE, intent.getStringExtra("title"));
                task.setValue(Task.REMOTE_ID, Long.parseLong(intent.getStringExtra("task_id")));
                task.setValue(Task.USER_ID, Task.USER_ID_UNASSIGNED);
                task.putTransitory(SyncFlags.ACTFM_SUPPRESS_SYNC, true);
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
                tagData.putTransitory(SyncFlags.ACTFM_SUPPRESS_SYNC, true);
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

            Intent launchIntent = new Intent(context, TaskListActivity.class);
            launchIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
            filter.customExtras.putBoolean(TagViewFragment.TOKEN_START_ACTIVITY, shouldLaunchActivity(intent));
            launchIntent.putExtras(filter.customExtras);

            return launchIntent;
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

}
