/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.weloveastrid.rmilk.MilkUtilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.BadTokenException;
import android.widget.TextView;

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
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
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

//        try {
//            JSONObject test = new JSONObject();
//            test.put("date", "09/26/12");
//            test.put("message", "Screens!");
//            test.put("type", "screen");
//            test.put("link", "Click me");
//            JSONArray screenArray = new JSONArray();
//            screenArray.put(ActFmLoginActivity.class.getName());
//            screenArray.put(EditPreferences.class.getName());
//            test.put("screens", screenArray);
//            updates.put(test);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        if(updates == null || updates.length() == 0)
            return;

        CharSequence message = buildUpdateMessage(updates);
        if(message.length() == 0)
            return;

        displayUpdateDialog(message);
    }

    private static interface DialogShower {
        void showDialog(Activity activity);
    }

    private void showDialog(DialogShower ds) {
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

    protected void displayUpdateDialog(CharSequence message) {
        if(activity == null)
            return;

        final DialogShower ds;
        if (message instanceof Spannable) {
            final TextView textView = new TextView(activity);
            textView.setText(message);
            textView.setTextSize(16);
            textView.setTextColor(activity.getResources().getColor(ThemeService.getDialogTextColor()));
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            ds = new DialogShower() {
                @Override
                public void showDialog(Activity a) {
                    final Dialog d = new AlertDialog.Builder(a)
                    .setTitle(R.string.UpS_updates_title)
                    .setView(textView)
                    .setPositiveButton(R.string.DLG_ok, null)
                    .create();
                    textView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            d.dismiss();
                        }
                    });
                    d.show();
                }
            };
        } else {
            String color = ThemeService.getDialogTextColorString();
            final String html = "<html><body style='color: " + color + "'>" +
                    message + "</body></html>";
            ds = new DialogShower() {
                @Override
                public void showDialog(Activity a) {
                    DialogUtilities.htmlDialog(a,
                            html, R.string.UpS_updates_title);
                }
            };
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showDialog(ds);
            }
        });

    }

    protected CharSequence buildUpdateMessage(JSONArray updates) {
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

            CharSequence toReturn;
            String type = update.optString("type", null);
            if ("screen".equals(type) || "pref".equals(type)) {
                String linkText = update.optString("link");
                Spannable span = Spannable.Factory.getInstance().newSpannable(message + "\n\n" + linkText);
                ClickableSpan click = getClickableSpanForUpdate(update, type);
                if (click == null)
                    continue;
                span.setSpan(click, span.length() - linkText.length(), span.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                toReturn = span;
            } else {
                StringBuilder builder = new StringBuilder();
                if(date != null)
                    builder.append("<b>" + date + "</b><br />");
                builder.append(message);
                builder.append("<br /><br />");
                toReturn = builder.toString();
            }

            if(messageAlreadySeen(date, message))
                continue;
            return toReturn;
        }
        return "";
    }

    private ClickableSpan getClickableSpanForUpdate(JSONObject update, String type) {
        if ("pref".equals(type)) {
            final String prefSpec = update.optString("prefSpec", null);
            if (prefSpec == null)
                return null;
            return new ClickableSpan() {
                @Override
                public void onClick(View widget) {
//                    Intent prefScreen = new Intent(activity, UpdateMessagePreference.class);
//                    prefScreen.putExtra(UpdateMessagePreference.TOKEN_PREF_SPEC, prefSpec);
//                    activity.startActivity(prefScreen);
                }
            };
        } else if ("screen".equals(type)) {
            try {
                JSONArray screens = update.getJSONArray("screens");
                if (screens.length() == 0)
                    return null;
                final ArrayList<String> screenList = new ArrayList<String>();
                for (int i = 0; i < screens.length(); i++) {
                    screenList.add(screens.getString(i));
                }
                return new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
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
                    "language=" + Locale.getDefault().getISO3Language() + "&" +
                    "market=" + Constants.MARKET_STRATEGY.strategyId() + "&" +
                    "actfm=" + (actFmPreferenceService.isLoggedIn() ? "1" : "0") + "&" +
                    "premium=" + (ActFmPreferenceService.isPremiumUser() ? "1" : "0")); //$NON-NLS-1$
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
