package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.andlib.utility.Preferences;

@SuppressWarnings("nls")
public class ActFmInvoker {

    /** NOTE: these values are development values & will not work on production */
    private static final String URL = "//10.0.2.2:3000/api/";
    private static final String APP_ID = "a4732a32859dbcd3e684331acd36432c";
    private static final String APP_SECRET = "e389bfc82a0d932332f9a8bd8203735f";

    public static final String PROVIDER_FACEBOOK = "facebook";
    public static final String PROVIDER_GOOGLE= "google";
    public static final String PROVIDER_PASSWORD = "password";

    private static final int API_VERSION = 6;

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
                "secret", secret);
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
        try {
            String request = createFetchUrl(method, getParameters);
            Log.e("act-fm-invoke", request);
            String response = restClient.get(request);
            Log.e("act-fm-invoke-response", response);
            JSONObject object = new JSONObject(response);
            if(object.getString("status").equals("error"))
                throw new ActFmServiceException(object.getString("message"));
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
            String request = createFetchUrl(method, getParameters);
            Log.e("act-fm-post", request);
            String response = restClient.post(request, data);
            Log.e("act-fm-post-response", response);
            JSONObject object = new JSONObject(response);
            if(object.getString("status").equals("error"))
                throw new ActFmServiceException(object.getString("message"));
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
    public String createFetchUrl(String method, Object... getParameters) throws UnsupportedEncodingException, NoSuchAlgorithmException {
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
        params.add(new Pair<String, Object>("time", System.currentTimeMillis() / 1000L));
        params.add(new Pair<String, Object>("api", API_VERSION));
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
        if (method.startsWith("/"))
            url = url.replaceFirst("/api/", "");
        if (Preferences.getBoolean(R.string.actfm_https_key, false))
            url = "https:" + url;
        else
            url = "http:" + url;

        StringBuilder requestBuilder = new StringBuilder(url).append(method).append('?');
        StringBuilder sigBuilder = new StringBuilder();
        for(Pair<String, Object> entry : params) {
            if(entry.getRight() == null)
                continue;

            String key = entry.getLeft();
            String value = entry.getRight().toString();
            String encoded = URLEncoder.encode(value, "UTF-8");

            requestBuilder.append(key).append('=').append(encoded).append('&');

            sigBuilder.append(key).append(value);
        }

        sigBuilder.append(APP_SECRET);
        String signature = DigestUtils.md5Hex(sigBuilder.toString());
        requestBuilder.append("sig").append('=').append(signature);
        return requestBuilder.toString();
    }

}
