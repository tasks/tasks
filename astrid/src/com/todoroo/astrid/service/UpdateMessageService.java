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

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;

/**
 * Notifies users when there are server updates
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class UpdateMessageService {

    private static final String URL = "http://localhost/update.php";

    private static final String PLUGIN_PDV = "pdv";
    private static final String PLUGIN_GTASKS = "gtasks";
    private static final String PLUGIN_RMILK = "rmilk";

    @Autowired protected RestClient restClient;
    @Autowired private GtasksPreferenceService gtasksPreferenceService;
    @Autowired private AddOnService addOnService;

    public UpdateMessageService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public void processUpdates() {
        if(shouldSkipUpdates())
            return;

        JSONArray updates = checkForUpdates();

        if(updates == null || updates.length() == 0)
            return;

        if(updatesHaveNotChanged(updates))
            return;

        StringBuilder builder = buildUpdateMessage(updates);
        if(builder.length() == 0)
            return;

        displayUpdateDialog(builder);
    }

    protected boolean shouldSkipUpdates() {
        Context context = ContextManager.getContext();
        return !(context instanceof Activity);
    }

    private boolean updatesHaveNotChanged(JSONArray updates) {
        String savedUpdates = AstridPreferences.getLatestUpdates();
        String latest = updates.toString();
        if(AndroidUtilities.equals(savedUpdates, latest))
            return true;
        AstridPreferences.setLatestUpdates(latest);
        return false;
    }

    protected void displayUpdateDialog(final StringBuilder builder) {
        final Activity activity = (Activity) ContextManager.getContext();
        if(activity == null)
            return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DialogUtilities.htmlDialog(activity,
                        builder.toString(), R.string.UpS_changelog_title);
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

            String date = update.optString("date");
            String message = update.optString("message");
            String plugin = update.optString("plugin");

            if(message == null)
                continue;
            if(plugin != null) {
                // handle internal plugin specially
                if(PLUGIN_PDV.equals(plugin)) {
                    if(!ProducteevUtilities.INSTANCE.isLoggedIn())
                        continue;
                }
                else if(PLUGIN_GTASKS.equals(plugin)) {
                    if(!gtasksPreferenceService.isLoggedIn())
                        continue;
                }
                else if(PLUGIN_RMILK.equals(plugin)) {
                    if(!MilkUtilities.INSTANCE.isLoggedIn())
                        continue;
                }
                else if(!addOnService.isInstalled(plugin)) {
                    continue;
                }
            }
            if(date != null)
                builder.append("<b>" + date + "</b><br />");
            builder.append(message);
            builder.append("<br /><br />");
        }
        return builder;
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

}
