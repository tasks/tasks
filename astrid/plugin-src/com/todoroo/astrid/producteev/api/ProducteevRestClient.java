package com.todoroo.astrid.producteev.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import com.todoroo.andlib.service.RestClient;
import com.todoroo.astrid.utility.Constants;

/**
 * RestClient allows Android to consume web requests.
 * <p>
 * Portions by Praeda:
 * http://senior.ceng.metu.edu.tr/2009/praeda/2009/01/11/a-simple
 * -restful-client-at-android/
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ProducteevRestClient implements RestClient {

    private static final int HTTP_OK = 200;

    private static final int TIMEOUT_MILLIS = 30000;

    private static HttpClient httpClient = null;

    private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the
         * BufferedReader.readLine() method. We iterate until the BufferedReader
         * return null which means there's no more data to read. Each line will
         * appended to a StringBuilder and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is), 16384);
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n"); //$NON-NLS-1$
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

    private synchronized static void initializeHttpClient() {
        if (httpClient == null) {
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_MILLIS);
            HttpConnectionParams.setSoTimeout(params, TIMEOUT_MILLIS);
            httpClient = new DefaultHttpClient(params);
        }
    }

    private String processHttpResponse(HttpResponse response) throws IOException, ApiServiceException {
        HttpEntity entity = response.getEntity();
        String body = null;
        if (entity != null) {
            InputStream contentStream = entity.getContent();
            try {
                body = convertStreamToString(contentStream);
            } finally {
                contentStream.close();
            }
        }
        if(Constants.DEBUG)
            System.err.println(body);

        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode != HTTP_OK || (body != null && body.startsWith("{\"error\":"))) { //$NON-NLS-1$
            ApiServiceException error;
            try {
                JSONObject errorObject = new JSONObject(body).getJSONObject("error"); //$NON-NLS-1$
                String errorMessage = errorObject.getString("message"); //$NON-NLS-1$

                if(statusCode == 403)
                    error = new ApiSignatureException(errorMessage);
                else if(statusCode == 401)
                    error = new ApiAuthenticationException(errorMessage);
                else
                    error = new ApiServiceException(errorMessage);
            } catch (Exception e) {
                if(statusCode == 401)
                    error = new ApiAuthenticationException(response.getStatusLine().getReasonPhrase());
                else
                    error = new ApiServiceException(response.getStatusLine() +
                        "\n" + body); //$NON-NLS-1$
            }
            throw error;
        }


        return body;
    }

    /**
     * Issue an HTTP GET for the given URL, return the response
     *
     * @param url url with url-encoded params
     * @return response, or null if there was no response
     * @throws IOException
     */
    public synchronized String get(String url) throws IOException {
        initializeHttpClient();

        if(Constants.DEBUG)
            System.err.println("GET: " + url); //$NON-NLS-1$ // (debug)

        try {
            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = httpClient.execute(httpGet);

            return processHttpResponse(response);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
    }

    /**
     * Issue an HTTP POST for the given URL, return the response
     *
     * @param url
     * @param data
     *            url-encoded data
     * @throws IOException
     */
    public synchronized String post(String url, HttpEntity data) throws IOException {
        initializeHttpClient();

        if(Constants.DEBUG)
            System.err.println("POST: " + url); //$NON-NLS-1$ // (debug)

        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(data);
            HttpResponse response = httpClient.execute(httpPost);

            return processHttpResponse(response);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
    }

    /**
     * Destroy and re-create http client
     */
    public void reset() {
        httpClient = null;
    }

}
