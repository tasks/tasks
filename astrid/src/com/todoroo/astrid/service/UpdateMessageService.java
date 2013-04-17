/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.BadTokenException;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.StoreObjectDao.StoreObjectCriteria;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.utility.Constants;

/**
 * Notifies users when there are server updates
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class UpdateMessageService {

    private static final String URL = "http://blog.astrid.com/updates";

    private static final String PLUGIN_GTASKS = "gtasks";

    @Autowired protected RestClient restClient;
    @Autowired private GtasksPreferenceService gtasksPreferenceService;
    @Autowired private ActFmPreferenceService actFmPreferenceService;
    @Autowired private AddOnService addOnService;
    @Autowired private StoreObjectDao storeObjectDao;

    private final Activity activity;

    public UpdateMessageService(Activity activity) {
        this.activity = activity;
        DependencyInjectionService.getInstance().inject(this);
    }

    public void processUpdates() {
        JSONArray updates = checkForUpdates();

        if(updates == null || updates.length() == 0)
            return;

        MessageTuple message = buildUpdateMessage(updates);
        if(message == null || message.message.length() == 0)
            return;

        displayUpdateDialog(message);
    }

    public static class MessageTuple {
        public String message = null;
        public List<String> linkText = new ArrayList<String>();
        public List<OnClickListener> click = new ArrayList<OnClickListener>();
    }

    private static interface DialogShower {
        void showDialog(Activity activity);
    }

    private void tryShowDialog(DialogShower ds) {
        try {
            ds.showDialog(activity);
        } catch (BadTokenException bt) {
            try {
                Activity current = (Activity) ContextManager.getContext();
                ds.showDialog(current);
            } catch (ClassCastException c) {
                // Oh well, context wasn't an activity
            } catch (BadTokenException bt2) {
                // Oh well, activity isn't running
            }
        }
    }

    protected void displayUpdateDialog(final MessageTuple message) {
        if(activity == null)
            return;

        if (message.linkText.size() > 0) {
            final DialogShower ds = new DialogShower() {
                @Override
                public void showDialog(Activity a) {
                    try {
                        final Dialog d = new Dialog(activity, R.style.ReminderDialog);
                        d.setContentView(R.layout.update_message_view);

                        // TODO: Make HTML message
                        WebView messageView = (WebView) d.findViewById(R.id.reminder_message);
                        String html = "<html><body>" + message.message + "</body></html>";
                        messageView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
                        messageView.setBackgroundColor(0);
                        d.findViewById(R.id.dismiss).setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                d.dismiss();
                            }
                        });


                        LinearLayout root = (LinearLayout) d.findViewById(R.id.reminder_root);
                        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
                        TypedValue themeColor = new TypedValue();
                        activity.getTheme().resolveAttribute(R.attr.asThemeTextColor, themeColor, false);
                        int color = activity.getResources().getColor(themeColor.data);
                        for (int i = 0; i < message.linkText.size(); i++) {
                            Button linkButton = new Button(activity);
                            linkButton.setText(message.linkText.get(i));
                            final OnClickListener click = message.click.get(i);
                            linkButton.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    click.onClick(v);
                                    d.dismiss();
                                }
                            });
                            LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, (int) (metrics.density * 35));
                            params.leftMargin = params.rightMargin = (int) (metrics.density * 5);
                            params.bottomMargin = (int) (metrics.density * 10);
                            linkButton.setTextColor(Color.WHITE);
                            linkButton.setTextSize(20);
                            linkButton.setBackgroundColor(color);
                            linkButton.setLayoutParams(params);
                            linkButton.setPadding(0, 0, 0, 0);

                            root.addView(linkButton);
                        }

                        d.show();
                    } catch (Exception e) {
                        // This should never ever crash
                    }
                }
            };
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tryShowDialog(ds);
                }
            });
        }

    }

    protected MessageTuple buildUpdateMessage(JSONArray updates) {
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

            MessageTuple toReturn = new MessageTuple();
            toReturn.message = message;
            String type = update.optString("type", null);
            if ("screen".equals(type) || "pref".equals(type)) {
                String linkText = update.optString("link");
                OnClickListener click = getClickListenerForUpdate(update, type);
                if (click == null)
                    continue;
                toReturn.linkText.add(linkText);
                toReturn.click.add(click);
            } else {
                JSONArray links = update.optJSONArray("links");
                if (links != null) {
                    for (int j = 0; j < links.length(); j++) {
                        JSONObject link = links.optJSONObject(j);
                        if (link == null)
                            continue;
                        String linkText = link.optString("title");
                        if (TextUtils.isEmpty(linkText))
                            continue;

                        final String url = link.optString("url");
                        OnClickListener click = new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                activity.startActivity(intent);
                            }
                        };

                        toReturn.linkText.add(linkText);
                        toReturn.click.add(click);
                    }
                }
            }

            if(messageAlreadySeen(date, message))
                continue;
            return toReturn;
        }
        return null;
    }

    private OnClickListener getClickListenerForUpdate(JSONObject update, String type) {
        if ("pref".equals(type)) {
            try {
                if (!update.has("action_list"))
                    return null;
                JSONArray prefSpec = update.getJSONArray("action_list");
                if (prefSpec.length() == 0)
                    return null;
                final String prefArray = prefSpec.toString();
                return new View.OnClickListener() {
                    @Override
                    public void onClick(View b) {
                        Intent prefScreen = new Intent(activity, UpdateMessagePreference.class);
                        prefScreen.putExtra(UpdateMessagePreference.TOKEN_PREFS_ARRAY, prefArray);
                        activity.startActivityForResult(prefScreen, 0);
                    }
                };
            } catch (JSONException e) {
                return null;
            }
        } else if ("screen".equals(type)) {
            try {
                if (!update.has("action_list"))
                    return null;
                JSONArray screens = update.getJSONArray("action_list");
                if (screens.length() == 0)
                    return null;
                final ArrayList<String> screenList = new ArrayList<String>();
                for (int i = 0; i < screens.length(); i++) {
                    String screen = screens.getString(i).trim();
                    if (!TextUtils.isEmpty(screen))
                        screenList.add(screen);
                }
                return new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent screenFlow = new Intent(activity, UpdateScreenFlow.class);
                        screenFlow.putStringArrayListExtra(UpdateScreenFlow.TOKEN_SCREENS, screenList);
                        activity.startActivity(screenFlow);
                    }
                };
            } catch (JSONException e) {
                return null;
            }
        }
        return null;
    }

    private boolean pluginConditionMatches(String plugin) {
        // handle internal plugin specially
        if(PLUGIN_GTASKS.equals(plugin))
            return gtasksPreferenceService.isLoggedIn();
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
            String url = URL + "?version=" + versionCode + "&" +
                    "language=" + Locale.getDefault().getISO3Language() + "&" +
                    "market=" + Constants.MARKET_STRATEGY.strategyId() + "&" +
                    "actfm=" + (actFmPreferenceService.isLoggedIn() ? "1" : "0") + "&" +
                    "premium=" + (ActFmPreferenceService.isPremiumUser() ? "1" : "0");
            String result = restClient.get(url); //$NON-NLS-1$
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
