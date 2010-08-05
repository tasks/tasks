package com.todoroo.astrid.producteev.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
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

    // --- authentication and time

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

    public String getToken() {
        return token;
    }

    /**
     * Gets Server time
     */
    public String time() throws IOException, ApiServiceException {
        JSONObject response = invokeGet("time.json");
        try {
            return response.getJSONObject("time").getString("value");
        } catch (JSONException e) {
            throw new ApiServiceException(e);
        }

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
     * @return task
     */
    public JSONObject tasksCreate(String title, Long idResponsible, Long idDashboard,
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
    public JSONArray tasksShowList(Long idDashboard, String since) throws ApiServiceException, IOException {
        return getResponse(invokeGet("tasks/show_list.json",
                "token", token,
                "id_dashboard", idDashboard,
                "since", since), "tasks");
    }

    /**
     * get a task
     *
     * @param idTask
     *
     * @return array tasks/view
     */
    public JSONObject tasksView(Long idTask) throws ApiServiceException, IOException {
        return invokeGet("tasks/view.json",
                "token", token,
                "id_task", idTask);
    }

    /**
     * change title of a task
     *
     * @param idTask
     * @param title
     *
     * @return array tasks/view
     */
    public JSONObject tasksSetTitle(long idTask, String title) throws ApiServiceException, IOException {
        return invokeGet("tasks/set_title.json",
                "token", token,
                "id_task", idTask,
                "title", title);
    }

    /**
     * set status of a task
     *
     * @param idTask
     * @param status (1 = UNDONE, 2 = DONE)
     *
     * @return array tasks/view
     */
    public JSONObject tasksSetStatus(long idTask, int status) throws ApiServiceException, IOException {
        return invokeGet("tasks/set_star.json",
                "token", token,
                "id_task", idTask,
                "status", status);
    }

    /**
     * set star status of a task
     *
     * @param idTask
     * @param star (0 to 5 stars)
     *
     * @return array tasks/view
     */
    public JSONObject tasksSetStar(long idTask, int star) throws ApiServiceException, IOException {
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
    public JSONObject tasksSetDeadline(long idTask, String deadline) throws ApiServiceException, IOException {
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
    public JSONObject tasksDelete(long idTask) throws ApiServiceException, IOException {
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
    public JSONArray tasksLabels(long idTask) throws ApiServiceException, IOException {
        return getResponse(invokeGet("tasks/labels.json",
                "token", token,
                "id_task", idTask), "labels");
    }

    /**
     * set a labels to a task
     *
     * @param idTask
     * @param idLabels
     *
     * @return array: tasks/view
     */
    public JSONObject tasksSetLabels(long idTask, long... idLabels) throws ApiServiceException, IOException {
        Object[] params = new Object[idLabels.length * 2 + 2];
        params[0] = "token";
        params[1] = token;
        for(int i = 0; i < idLabels.length; i++) {
            params[i*2 + 2] = "id_label[]";
            params[i*2 + 3] = idLabels[i];
        }

        return invokeGet("tasks/set_label.json", params);
    }

    /**
     * set a labels to a task
     *
     * @param idTask
     * @param idLabel
     *
     * @return array: tasks/view
     */
    public JSONObject tasksUnsetLabels(long idTask, long... idLabels) throws ApiServiceException, IOException {
        Object[] params = new Object[idLabels.length * 2 + 2];
        params[0] = "token";
        params[1] = token;
        for(int i = 0; i < idLabels.length; i++) {
            params[i*2 + 2] = "id_label[]";
            params[i*2 + 3] = idLabels[i];
        }

        return invokeGet("tasks/unset_label.json", params);
    }

    /**
     * create a note attached to a task
     *
     * @param idTask
     * @param message
     *
     * @return array tasks::note_view
     */
    public JSONObject tasksNoteCreate(long idTask, String message) throws ApiServiceException, IOException {
        return invokeGet("tasks/note_create.json",
                "token", token,
                "id_task", idTask,
                "message", message);
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
    public JSONArray labelsShowList(long idDashboard, String since) throws ApiServiceException, IOException {
        return getResponse(invokeGet("labels/show_list.json",
                "token", token,
                "id_dashboard", idDashboard,
                "since", since), "labels");
    }

    /**
     * create a label
     *
     * @param idDashboard
     * @param title
     *
     * @return array: labels/view
     */
    public JSONArray labelsCreate(long idDashboard, String title) throws ApiServiceException, IOException {
        return getResponse(invokeGet("labels/create.json",
                "token", token,
                "id_dashboard", idDashboard,
                "title", title), "labels");
    }

    // --- users

    /**
     * get a user
     *
     * @param idColleague
     *
     * @return array information about the user
     */
    public JSONObject usersView(Long idColleague) throws ApiServiceException, IOException {
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
            if(response.startsWith("DEBUG MESSAGE"))
                throw new ApiServiceException(response);
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
        System.err.println("sigbuilder " + sigBuilder);
        byte[] digest = MessageDigest.getInstance("MD5").digest(sigBuilder.toString().getBytes("UTF-8"));
        String signature = new BigInteger(1, digest).toString(16);
        requestBuilder.append("api_sig").append('=').append(signature);
        return requestBuilder.toString();
    }

    /**
     * Helper method to get a field out or throw an api exception
     * @param response
     * @param field
     * @return
     * @throws ApiResponseParseException
     */
    private JSONArray getResponse(JSONObject response, String field) throws ApiResponseParseException {
        try {
            return response.getJSONArray(field);
        } catch (JSONException e) {
            throw new ApiResponseParseException(e);
        }
    }

}
