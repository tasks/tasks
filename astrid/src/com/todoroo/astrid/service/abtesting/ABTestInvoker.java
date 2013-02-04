/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service.abtesting;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.service.StatisticsService;

/**
 * Invoker for communicating with the Astrid Analytics server
 * @author Sam
 *
 */
@SuppressWarnings("nls")
public class ABTestInvoker {

    /** NOTE: these values are development values and will not work on production */
    private static final String URL = "http://analytics.astrid.com/api/2/";
    public static final String AB_RETENTION_METHOD = "ab_retention";
    public static final String AB_ACTIVATION_METHOD = "ab_activation";
    private static final String ACQUISITION_METHOD = "acquisition";
    private static final String API_KEY = "ryyubd";
    private static final String API_SECRET = "q9ef3i";

    private static final String PREF_REPORTED_ACQUISITION = "p_reported_acq";

    @Autowired private RestClient restClient;

    public ABTestInvoker() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public void reportAcquisition() {
        if (!Preferences.getBoolean(PREF_REPORTED_ACQUISITION, false) &&
                !StatisticsService.dontCollectStatistics()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpEntity postData = createPostData(null);
                        restClient.post(URL + ACQUISITION_METHOD, postData);
                        Preferences.setBoolean(PREF_REPORTED_ACQUISITION, true);
                    } catch (IOException e) {
                        // Ignored
                    }
                }
            }).start();
        }
    }

    /**
     * Posts the payload to the analytics server
     * @param payload - JSONArray of data points. Created by the
     * helper method in ABTestReportingService
     * @return
     * @throws IOException
     */
    public JSONObject post(String method, JSONArray payload) throws IOException {
        try {
            HttpEntity postData  = createPostData(payload);
            String response = restClient.post(URL + method, postData);
            JSONObject object = new JSONObject(response);
            if (object.getString("status").equals("error")) {
                throw new IOException("Error reporting ABTestEvent: " +
                        object.optString("message"));
            }
            return object;
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }

    }

    /**
     * Converts the JSONArray payload into an HTTPEntity suitable for
     * POSTing.
     * @param payload
     * @return
     */
    private HttpEntity createPostData(JSONArray payload) throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("apikey", API_KEY));

        if (payload != null)
            params.add(new BasicNameValuePair("payload", payload.toString()));

        StringBuilder sigBuilder = new StringBuilder();
        for(NameValuePair entry : params) {
            if(entry.getValue() == null)
                continue;

            String key = entry.getName();
            String value = entry.getValue();

            sigBuilder.append(key).append(value);
        }

        sigBuilder.append(API_SECRET);
        String signature = DigestUtils.md5Hex(sigBuilder.toString());
        params.add(new BasicNameValuePair("sig", signature));

        try {
            return new UrlEncodedFormEntity(params, HTTP.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IOException("Unsupported URL encoding");
        }
    }

}
