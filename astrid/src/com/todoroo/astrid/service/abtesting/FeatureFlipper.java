package com.todoroo.astrid.service.abtesting;

import java.io.IOException;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class FeatureFlipper {

    private static final String URL = "http://weloveastrid.com/settings.php";

    private static final String KEY_SETTING_KEY = "key";
    private static final String KEY_SET_OPTION = "setOption";
    private static final String KEY_PROBABILITIES = "probabilities";

    @Autowired private RestClient restClient;
    @Autowired private ABChooser abChooser;
    @Autowired private ABTests abTests;

    public FeatureFlipper() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Requests a JSON array containing feature settings override data,
     * parses the result, and updates the AB settings for the corresponding features
     * @throws JSONException
     */
    public synchronized void updateFeatures() {
        JSONArray settingsBundle = requestOverrideSettings();
        if (settingsBundle == null || settingsBundle.length() == 0)
            return;

        for (int i = 0; i < settingsBundle.length(); i++) {
            try {
                JSONObject settings = settingsBundle.getJSONObject(i);
                String key = settings.getString(KEY_SETTING_KEY);

                if (settings.has(KEY_SET_OPTION)) {
                    int option = settings.getInt(KEY_SET_OPTION);
                    abChooser.setChoiceForTest(key, option);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private JSONArray requestOverrideSettings() {
        PackageManager pm = ContextManager.getContext().getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(Constants.PACKAGE, PackageManager.GET_META_DATA);
            int versionCode = pi.versionCode;
            String result = restClient.get(URL + "?version=" + versionCode + "&" +
                    "language=" + Locale.getDefault().getISO3Language());
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
