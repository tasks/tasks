/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.TimeZone;


public class ActFmInvoker {

    /**
     * NOTE: these values are development values & will not work on production
     */
    private static final String URL = "//10.0.2.2:3000/api/";
    private static final String APP_ID = "a4732a32859dbcd3e684331acd36432c";
    private static final String APP_SECRET = "e389bfc82a0d932332f9a8bd8203735f";

    public static final String PROVIDER_PASSWORD = "password";

    private static final int API_VERSION = 7;

    @Autowired
    private RestClient restClient;

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
     *
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
                                   String secret) throws IOException {
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
     * @param method        API method to invoke
     * @param getParameters Name/Value pairs. Values will be URL encoded.
     * @return response object
     */
    public JSONObject invoke(String method, Object... getParameters) throws IOException {
        return new JSONObject();
    }
}
