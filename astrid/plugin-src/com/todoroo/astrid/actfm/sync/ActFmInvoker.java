/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class ActFmInvoker {

    /** NOTE: these values are development values & will not work on production */
    private static final String URL = "//192.168.0.184:3000/api/";
    private static final String APP_ID = "a4732a32859dbcd3e684331acd36432c";
    private static final String APP_SECRET = "e389bfc82a0d932332f9a8bd8203735f";

    public static final String PROVIDER_FACEBOOK = "facebook";
    public static final String PROVIDER_GOOGLE= "google";
    public static final String PROVIDER_PASSWORD = "password";

    private static final int API_VERSION = 7;

    public static final boolean SYNC_DEBUG = Constants.DEBUG || true;

    @Autowired private RestClient restClient;

    private String token = null;

    // --- initialization, getters, setters

    /**
     * Create new api invoker service without a token
     */
    public ActFmInvoker() {
        //
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Create new api invoker service with a token
     * @token access token
     */
    public ActFmInvoker(String token) {
        this.token = token;
        DependencyInjectionService.getInstance().inject(this);
    }

    public String getToken() {
        return token;
    }

    // --- special method invocations

    /**
     * Authentication user with Act.fm server, returning a token
     */
    public JSONObject authenticate(String email, String firstName, String lastName, String provider,
            String secret) throws ActFmServiceException, IOException {
        JSONObject result = invoke(
                "user_signin",
                "email", email,
                "first_name", firstName,
                "last_name", lastName,
                "provider", provider,
                "secret", secret,
                "timezone", TimeZone.getDefault().getID());
        try {
            token = result.getString("token");
        } catch (JSONException e) {
            throw new IOException(e.toString());
        }
        return result;
    }

    // --- invocation

    /**
     * Invokes API method using HTTP GET
     *
     * @param method
     *          API method to invoke
     * @param getParameters
     *          Name/Value pairs. Values will be URL encoded.
     * @return response object
     */
    public JSONObject invoke(String method, Object... getParameters) throws IOException,
            ActFmServiceException {
        return invokeWithApi(null, method, getParameters);
    }

    /**
     * Invokes API method using HTTP GET
     *
     * @param method
     *          API method to invoke
     * @param getParameters
     *          Name/Value pairs. Values will be URL encoded.
     * @return response object
     */
    public JSONObject invokeWithApi(String api, String method, Object... getParameters) throws IOException,
    ActFmServiceException {
        try {
            String request = createFetchUrl(api, method, getParameters);

            if (SYNC_DEBUG)
                Log.e("act-fm-invoke", request);

            String response = restClient.get(request);
            JSONObject object = new JSONObject(response);

            if (SYNC_DEBUG)
                AndroidUtilities.logJSONObject("act-fm-invoke-response", object);
            if(object.getString("status").equals("error"))
                throw new ActFmServiceException(object.getString("message"), object);
            return object;
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invokes API method using HTTP POST
     *
     * @param method
     *          API method to invoke
     * @param data
     *          data to transmit
     * @param getParameters
     *          Name/Value pairs. Values will be URL encoded.
     * @return response object
     */
    public JSONObject post(String method, HttpEntity data, Object... getParameters) throws IOException,
    ActFmServiceException {
        try {
            String request = createFetchUrl(null, method, getParameters);

            if (SYNC_DEBUG)
                Log.e("act-fm-post", request);

            String response = restClient.post(request, data);
            JSONObject object = new JSONObject(response);

            if (SYNC_DEBUG)
                AndroidUtilities.logJSONObject("act-fm-post-response", object);

            if(object.getString("status").equals("error"))
                throw new ActFmServiceException(object.getString("message"), object);
            return object;
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject postSync(JSONArray data, String token) throws IOException,
    ActFmServiceException {
        try {
            String dataString = data.toString();
            String timeString = DateUtilities.timeToIso8601(DateUtilities.now(), true);

            String request = createFetchUrl("api/" + API_VERSION, "synchronize", "token", token, "data", dataString, "time", timeString);
            if (SYNC_DEBUG)
                Log.e("act-fm-post", request);
            List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
            pairs.add(new BasicNameValuePair("token", token));
            pairs.add(new BasicNameValuePair("data", data.toString()));
            pairs.add(new BasicNameValuePair("time", timeString));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, HTTP.UTF_8);

            String response = restClient.post(request, entity);
            JSONObject object = new JSONObject(response);

            if (SYNC_DEBUG)
                AndroidUtilities.logJSONObject("act-fm-post-response", object);

            if(object.getString("status").equals("error"))
                throw new ActFmServiceException(object.getString("message"), object);
            return object;
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a URL for invoking an HTTP GET/POST on the given method
     * @param method
     * @param getParameters
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    private String createFetchUrl(String api, String method, Object... getParameters) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        ArrayList<Pair<String, Object>> params = new ArrayList<Pair<String, Object>>();
        for(int i = 0; i < getParameters.length; i += 2) {
            if(getParameters[i+1] instanceof ArrayList) {
                ArrayList<?> list = (ArrayList<?>) getParameters[i+1];
                for(int j = 0; j < list.size(); j++)
                    params.add(new Pair<String, Object>(getParameters[i].toString() + "[]",
                            list.get(j)));
            } else
                params.add(new Pair<String, Object>(getParameters[i].toString(), getParameters[i+1]));
        }
        params.add(new Pair<String, Object>("app_id", APP_ID));
        boolean syncMethod = "synchronize".equals(method);

        if (!syncMethod)
            params.add(new Pair<String, Object>("time", System.currentTimeMillis() / 1000L));
        if(token != null) {
            boolean foundTokenKey = false;
            for (Pair<String, Object> curr : params) {
                if (curr.getLeft().equals("token")) {
                    foundTokenKey = true;
                    break;
                }
            }
            if (!foundTokenKey)
                params.add(new Pair<String, Object>("token", token));
        }

        Collections.sort(params, new Comparator<Pair<String, Object>>() {
            @Override
            public int compare(Pair<String, Object> object1,
                    Pair<String, Object> object2) {
                int result = object1.getLeft().compareTo(object2.getLeft());
                if(result == 0)
                    return object1.getRight().toString().compareTo(object2.getRight().toString());
                return result;
            }
        });

        String url = URL;
        boolean customApi = false;
        if (api != null) {
            customApi = true;
            url = url.replace("api", api);
        }
        if (Preferences.getBoolean(R.string.actfm_https_key, false))
            url = "https:" + url;
        else
            url = "http:" + url;

        StringBuilder requestBuilder = new StringBuilder(url);
        if (!customApi)
            requestBuilder.append(API_VERSION).append("/");
        requestBuilder.append(method).append('?');
        StringBuilder sigBuilder = new StringBuilder(method);
        for(Pair<String, Object> entry : params) {
            if(entry.getRight() == null)
                continue;

            String key = entry.getLeft();
            String value = entry.getRight().toString();
            String encoded = URLEncoder.encode(value, "UTF-8");

            if (!syncMethod || "app_id".equals(key));
                requestBuilder.append(key).append('=').append(encoded).append('&');

            sigBuilder.append(key).append(value);
        }

        sigBuilder.append(APP_SECRET);
        String signature = DigestUtils.md5Hex(sigBuilder.toString());
        requestBuilder.append("sig").append('=').append(signature);
        return requestBuilder.toString();
    }

}
