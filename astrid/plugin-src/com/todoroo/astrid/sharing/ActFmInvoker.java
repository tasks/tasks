package com.todoroo.astrid.sharing;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class ActFmInvoker {

    /** NOTE: these values are development values & will not work on production */
    private static final String URL = "http://pre.act.fm/api";
    private static final String APP_ID = "bf6170638298af8ed9a8c79995b1fc0f";
    private static final String APP_SECRET = "e389bfc82a0d932332f9a8bd8203735f";

    public static final String PROVIDER_FACEBOOK = "facebook";
    public static final String PROVIDER_GOOGLE= "google";

    @Autowired private RestClient restClient;

    private String token = null;

    // --- initialization, getters, setters

    /**
     * Create new api invoker service without a token
     */
    public ActFmInvoker() {
        //
    }

    /**
     * Create new api invoker service with a token
     * @token access token
     */
    public ActFmInvoker(String token) {
        this.token = token;
    }

    public boolean hasToken() {
        return token != null;
    }

    // --- special method invocations

    /**
     * Authentication user with Act.fm server, returning a token
     */
    public JSONObject authenticate(String email, String name, String provider,
            String secret) throws ActFmServiceException, IOException {
        JSONObject result = invoke(
                "user_create",
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
            if(Constants.DEBUG)
                Log.e("act-fm-invoke", request);
            String response = restClient.get(request);
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
        for(int i = 0; i < getParameters.length; i += 2)
            params.add(new Pair<String, Object>(getParameters[i].toString(), getParameters[i+1]));
        params.add(new Pair<String, Object>("app_id", APP_ID));

        Collections.sort(params, new Comparator<Pair<String, Object>>() {
            @Override
            public int compare(Pair<String, Object> object1,
                    Pair<String, Object> object2) {
                return object1.getLeft().compareTo(object2.getLeft());
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

            if(!key.endsWith("[]"))
                sigBuilder.append(key).append(value);
        }

        sigBuilder.append(APP_SECRET);
        String signature = DigestUtils.md5Hex(sigBuilder.toString());
        requestBuilder.append("sig").append('=').append(signature);
        return requestBuilder.toString();
    }

}
