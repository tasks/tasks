package com.timsu.astrid;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.TagViewActivity;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;

@SuppressWarnings("nls")
public class C2DMReceiver extends BroadcastReceiver {

    public static final String C2DM_SENDER = "c2dm@astrid.com"; //$NON-NLS-1$

    private static final String PREF_REGISTRATION = "c2dm_key";

    @Autowired ActFmSyncService actFmSyncService;
    @Autowired TagDataService tagDataService;
    @Autowired UpdateDao updateDao;

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
                    handleMessage(intent);
                }
            }).start();
         }
     }

    /** Handle message. Run on separate thread. */
    private void handleMessage(Intent intent) {
        String message = intent.getStringExtra("alert");
        Context context = ContextManager.getContext();

        Intent notifyIntent = null;
        int notifId;

        // fetch data
        if(intent.hasExtra("tag_id")) {
            notifyIntent = createTagIntent(context, intent);
            notifId = (int) Long.parseLong(intent.getStringExtra("tag_id"));
        } else {
            notifId = Constants.NOTIFICATION_ACTFM;
        }

        if(notifyIntent == null)
            notifyIntent = ShortcutActivity.createIntent(CoreFilterExposer.buildInboxFilter(context.getResources()));

        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                Constants.NOTIFICATION_ACTFM, notifyIntent, 0);

        // create notification
        NotificationManager nm = new AndroidNotificationManager(ContextManager.getContext());
        Notification notification = new Notification(R.drawable.notif_pink_alarm,
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

    private Intent createTagIntent(final Context context, final Intent intent) {
        TodorooCursor<TagData> cursor = tagDataService.query(
                Query.select(TagData.PROPERTIES).where(TagData.REMOTE_ID.eq(
                        intent.getStringExtra("tag_id"))));
        try {
            final TagData tagData = new TagData();
            if(cursor.getCount() == 0) {
                tagData.setValue(TagData.NAME, intent.getStringExtra("title"));
                tagData.setValue(TagData.REMOTE_ID, Long.parseLong(intent.getStringExtra("tag_id")));
                Flags.set(Flags.SUPPRESS_SYNC);
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
            if(intent.hasExtra("activity_id")) {
                filter.customExtras.putInt(TagViewActivity.EXTRA_START_TAB, 1);

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
                    update.setValue(Update.TAG, tagData.getId());
                    updateDao.createNew(update);
                } catch (JSONException e) {
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
