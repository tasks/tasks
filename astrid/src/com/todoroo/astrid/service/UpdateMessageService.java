package com.todoroo.astrid.service;

import java.io.IOException;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.weloveastrid.rmilk.MilkUtilities;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.view.WindowManager.BadTokenException;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.StoreObjectDao.StoreObjectCriteria;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.utility.Constants;

/**
 * Notifies users when there are server updates
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class UpdateMessageService {

    private static final String URL = "http://www.weloveastrid.com/updates.php";

    private static final String PLUGIN_PDV = "pdv";
    private static final String PLUGIN_GTASKS = "gtasks";
    private static final String PLUGIN_RMILK = "rmilk";

    @Autowired protected RestClient restClient;
    @Autowired private GtasksPreferenceService gtasksPreferenceService;
    @Autowired private AddOnService addOnService;
    @Autowired private StoreObjectDao storeObjectDao;

    private Context context;

    public UpdateMessageService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public void processUpdates(Context thisContext) {
        context = thisContext;

        if(shouldSkipUpdates())
            return;

        JSONArray updates = checkForUpdates();

        if(updates == null || updates.length() == 0)
            return;

        StringBuilder builder = buildUpdateMessage(updates);
        if(builder.length() == 0)
            return;

        displayUpdateDialog(builder);
    }

    protected boolean shouldSkipUpdates() {
        return !(context instanceof Activity);
    }

    protected void displayUpdateDialog(StringBuilder builder) {
        final Activity activity = (Activity) context;
        if(activity == null)
            return;

        String color = (AndroidUtilities.getSdkVersion() >= 14 ? "black" : "white");
        final String html = "<html><body style='color: " + color + "'>" +
            builder.append("</body></html>").toString();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    DialogUtilities.htmlDialog(activity,
                            html, R.string.UpS_updates_title);
                } catch (BadTokenException bt) {
                    try {
                        Activity current = (Activity) ContextManager.getContext();
                        DialogUtilities.htmlDialog(current,
                                html, R.string.UpS_updates_title);
                    } catch (ClassCastException c) {
                        // Oh well, context wasn't an activity
                    } catch (BadTokenException bt2) {
                        // Oh well, activity isn't running
                    }
                }
            }
        });
    }

    protected StringBuilder buildUpdateMessage(JSONArray updates) {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < updates.length(); i++) {
            JSONObject update;
            try {
                update = updates.getJSONObject(i);
            } catch (JSONException e) {
                continue;
            }

            String date = update.optString("date", null);
            String message = update.optString("message", null);
            String plugin = update.optString("plugin", null);
            String notPlugin = update.optString("notplugin", null);

            if(message == null)
                continue;
            if(plugin != null) {
                if(!pluginConditionMatches(plugin))
                    continue;
            }
            if(notPlugin != null) {
                if(pluginConditionMatches(notPlugin))
                    continue;
            }

            if(messageAlreadySeen(date, message))
                continue;

            if(date != null)
                builder.append("<b>" + date + "</b><br />");
            builder.append(message);
            builder.append("<br /><br />");
        }
        return builder;
    }

    private boolean pluginConditionMatches(String plugin) {
        // handle internal plugin specially
        if(PLUGIN_PDV.equals(plugin)) {
            return ProducteevUtilities.INSTANCE.isLoggedIn();
        }
        else if(PLUGIN_GTASKS.equals(plugin)) {
            return gtasksPreferenceService.isLoggedIn();
        }
        else if(PLUGIN_RMILK.equals(plugin)) {
            return MilkUtilities.INSTANCE.isLoggedIn();
        }
        else
            return addOnService.isInstalled(plugin);
    }

    private boolean messageAlreadySeen(String date, String message) {
        if(date != null)
            message = date + message;
        String hash = AndroidUtilities.md5(message);

        TodorooCursor<StoreObject> cursor = storeObjectDao.query(Query.select(StoreObject.ID).
                where(StoreObjectCriteria.byTypeAndItem(UpdateMessage.TYPE, hash)));
        try {
            if(cursor.getCount() > 0)
                return true;
        } finally {
            cursor.close();
        }

        StoreObject newUpdateMessage = new StoreObject();
        newUpdateMessage.setValue(StoreObject.TYPE, UpdateMessage.TYPE);
        newUpdateMessage.setValue(UpdateMessage.HASH, hash);
        storeObjectDao.persist(newUpdateMessage);
        return false;
    }

    private JSONArray checkForUpdates() {
        PackageManager pm = ContextManager.getContext().getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(Constants.PACKAGE, PackageManager.GET_META_DATA);
            int versionCode = pi.versionCode;
            String result = restClient.get(URL + "?version=" + versionCode + "&" +
                    "language=" + Locale.getDefault().getISO3Language()); //$NON-NLS-1$
            if(TextUtils.isEmpty(result))
                return null;

            return new JSONArray(result);
        } catch (IOException e) {
            return null;
        } catch (NameNotFoundException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }
    }

    /** store object for messages a user has seen */
    static class UpdateMessage {

        /** type*/
        public static final String TYPE = "update-message"; //$NON-NLS-1$

        /** message contents */
        public static final StringProperty HASH = new StringProperty(StoreObject.TABLE,
                StoreObject.ITEM.name);

   }


}
