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

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.Pair;

@SuppressWarnings("nls")
public class ActFmInvoker {

    /** NOTE: these values are development values & will not work on production */
    private static final String URL = "http://10.0.2.2:3000/api/";
    private static final String APP_ID = "bf6170638298af8ed9a8c79995b1fc0f";
    private static final String APP_SECRET = "e389bfc82a0d932332f9a8bd8203735f";

    public static final String PROVIDER_FACEBOOK = "facebook";
    public static final String PROVIDER_GOOGLE= "google";
    public static final String PROVIDER_PASSWORD = "password";

    private static final int API_VERSION = 2;

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
    public JSONObject authenticate(String email, String name, String provider,
            String secret) throws ActFmServiceException, IOException {
        JSONObject result = invoke(
                "user_signin",
                "email", email,
                "name", name,
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
    private String createFetchUrl(String method, Object... getParameters) throws UnsupportedEncodingException, NoSuchAlgorithmException {
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
        if(token != null)
            params.add(new Pair<String, Object>("token", token));

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

        StringBuilder requestBuilder = new StringBuilder(URL).append(method).append('?');
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
        System.err.println("SIG: " + sigBuilder);
        String signature = DigestUtils.md5Hex(sigBuilder.toString());
        requestBuilder.append("sig").append('=').append(signature);
        return requestBuilder.toString();
    }

}
