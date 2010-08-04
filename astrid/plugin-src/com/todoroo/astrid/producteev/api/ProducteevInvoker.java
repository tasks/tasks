package com.todoroo.astrid.producteev.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.todoroo.andlib.service.RestClient;

@SuppressWarnings("nls")
public class ProducteevInvoker {

    private final String URL = "https://api.producteev.com/";

    private final String apiKey;
    private final String apiSecret;
    private String token = null;

    /**
     * Create new producteev service
     * @param apiKey
     * @param apiSecret
     */
    public ProducteevInvoker(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    // --- authentication

    /**
     * Authenticate the given user
     */
    public void authenticate(String email, String password) throws IOException, ApiServiceException {
        JSONObject response = invokeGet("users/login.json",
                "email", email, "password", password);
        try {
            token = response.getJSONObject("login").getString("token");
        } catch (JSONException e) {
            throw new ApiServiceException(e);
        }

    }

    public boolean hasToken() {
        return token != null;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // --- tasks

    /**
     * create a task
     *
     * @param title
     * @param idResponsible (optional)
     * @param idDashboard (optional);
     * @param deadline (optional)
     * @param reminder (optional) 0 = "None", 1 = "5 minutes before", 2 = "15 minutes before", 3 = "30 minutes before", 4 = "1 hour before", 5 = "2 hours before", 6 = "1 day before", 7 = "2 days before", 8 = "on date of event"
     * @param status (optional) (1 = UNDONE, 2 = DONE)
     * @param star (optional) (0 to 5 stars)
     *
     * @return array tasks/view
     */
    public JSONObject tasksCreate(String title, Integer idResponsible, Integer idDashboard,
            String deadline, Integer reminder, Integer status, Integer star) throws ApiServiceException, IOException {
        return invokeGet("tasks/create.json",
                "token", token,
                "title", title,
                "id_responsible", idResponsible,
                "id_dashboard", idDashboard,
                "deadline", deadline,
                "reminder", reminder,
                "status", status,
                "star", star);
    }

    /**
     * show list
     *
     * @param idResponsible (optional) if null return every task for current user
     * @param since (optional) if not null, the function only returns tasks modified or created since this date
     *
     * @return array tasks/view
     */
    public JSONObject tasksShowList(Integer idDashboard, String since) throws ApiServiceException, IOException {
        return invokeGet("tasks/show_list.json",
                "token", token,
                "id_dashboard", idDashboard,
                "since", since);
    }

    /**
     * change title of a task
     *
     * @param idTask
     * @param title
     *
     * @return array tasks/view
     */
    public JSONObject tasksSetTitle(int idTask, String title) throws ApiServiceException, IOException {
        return invokeGet("tasks/set_title.json",
                "token", token,
                "id_task", idTask,
                "title", title);
    }

    /**
     * set star status of a task
     *
     * @param idTask
     * @param star (0 to 5 stars)
     *
     * @return array tasks/view
     */
    public JSONObject tasksSetStar(int idTask, boolean star) throws ApiServiceException, IOException {
        return invokeGet("tasks/set_star.json",
                "token", token,
                "id_task", idTask,
                "star", star);
    }

    /**
     * set a deadline
     *
     * @param idTask
     * @param deadline
     *
     * @return array tasks/view
     */
    public JSONObject tasksSetDeadline(int idTask, String deadline) throws ApiServiceException, IOException {
        return invokeGet("tasks/set_deadline.json",
                "token", token,
                "id_task", idTask,
                "deadline", deadline);
    }

    /**
     * delete a task
     *
     * @param idTask
     *
     * @return array with the result = (Array("stats" => Array("result" => "TRUE|FALSE"))
     */
    public JSONObject tasksDelete(int idTask) throws ApiServiceException, IOException {
        return invokeGet("tasks/delete.json",
                "token", token,
                "id_task", idTask);
    }

    /**
     * get labels assigned to a task
     *
     * @param idTask
     *
     * @return array: list of labels/view
     */
    public JSONObject tasksLabels(int idTask) throws ApiServiceException, IOException {
        return invokeGet("tasks/labels.json",
                "token", token,
                "id_task", idTask);
    }

    /**
     * set a labels to a task
     *
     * @param idTask
     * @param idLabel
     *
     * @return array: tasks/view
     */
    public JSONObject tasksSetLabel(int idTask, int idLabel) throws ApiServiceException, IOException {
        return invokeGet("tasks/set_label.json",
                "token", token,
                "id_task", idTask,
                "id_label", idLabel);
    }

    /**
     * set a labels to a task
     *
     * @param idTask
     * @param idLabel
     *
     * @return array: tasks/view
     */
    public JSONObject tasksUnsetLabel(int idTask, int idLabel) throws ApiServiceException, IOException {
        return invokeGet("tasks/unset_label.json",
                "token", token,
                "id_task", idTask,
                "id_label", idLabel);
    }

    // --- labels

    /**
     * get every label for a given dashboard
     *
     * @param idTask
     * @param idLabel
     *
     * @return array: labels/view
     */
    public JSONObject labelsShowList(int idDashboard, String since) throws ApiServiceException, IOException {
        return invokeGet("labels/show_list.json",
                "token", token,
                "id_dashboard", idDashboard,
                "since", since);
    }

    /**
     * create a task
     *
     * @param idDashboard
     * @param title
     *
     * @return array: labels/view
     */
    public JSONObject labelsCreate(int idDashboard, String title) throws ApiServiceException, IOException {
        return invokeGet("labels/create.json",
                "token", token,
                "id_dashboard", idDashboard,
                "title", title);
    }

    // --- users

    /**
     * get a user
     *
     * @param idColleague
     *
     * @return array information about the user
     */
    public JSONObject usersView(Integer idColleague) throws ApiServiceException, IOException {
        return invokeGet("users/view.json",
                "token", token,
                "id_colleague", idColleague);
    }

    // --- invocation

    private final RestClient restClient = new ProducteevRestClient();

    /**
     * Invokes API method using HTTP GET
     *
     * @param method
     *          API method to invoke
     * @param getParameters
     *          Name/Value pairs. Values will be URL encoded.
     * @return response object
     */
    private JSONObject invokeGet(String method, Object... getParameters)
            throws IOException, ApiServiceException {
        try {
            String request = createFetchUrl(method, getParameters);
            String response = restClient.get(request);
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new ApiResponseParseException(e);
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
        TreeMap<String, Object> treeMap = new TreeMap<String, Object>();
        for(int i = 0; i < getParameters.length; i += 2)
            treeMap.put(getParameters[i].toString(), getParameters[i+1]);
        treeMap.put("api_key", apiKey);

        StringBuilder requestBuilder = new StringBuilder(URL).append(method).append('?');
        StringBuilder sigBuilder = new StringBuilder();
        for(Map.Entry<String, Object> entry : treeMap.entrySet()) {
            if(entry.getValue() == null)
                continue;

            String key = entry.getKey();
            String value = entry.getValue().toString();
            String encoded = URLEncoder.encode(value, "UTF-8");

            requestBuilder.append(key).append('=').append(encoded).append('&');
            sigBuilder.append(key).append(value);
        }

        sigBuilder.append(apiSecret);
        byte[] digest = MessageDigest.getInstance("MD5").digest(sigBuilder.toString().getBytes("UTF-8"));
        String signature = new BigInteger(1, digest).toString(16);
        requestBuilder.append("api_sig").append('=').append(signature);
        return requestBuilder.toString();
    }

}
