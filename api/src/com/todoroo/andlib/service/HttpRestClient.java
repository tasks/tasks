package com.todoroo.andlib.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

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
public class HttpRestClient implements RestClient {

    private static final int HTTP_UNAVAILABLE_END = 599;
    private static final int HTTP_UNAVAILABLE_START = 500;
    private static final int HTTP_OK = 200;

    private static final int TIMEOUT_MILLIS = 30000;

    private static WeakReference<HttpClient> httpClient = null;

    protected boolean debug = false;

    public HttpRestClient() {
        DependencyInjectionService.getInstance().inject(this);
    }

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

    private synchronized static HttpClient getClient() {
        if (httpClient == null || httpClient.get() == null) {
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_MILLIS);
            HttpConnectionParams.setSoTimeout(params, TIMEOUT_MILLIS);
            HttpClient client = new DefaultHttpClient(params);
            httpClient = new WeakReference<HttpClient>(client);
            return client;
        } else {
            return httpClient.get();
        }
    }

    private String processHttpResponse(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode >= HTTP_UNAVAILABLE_START && statusCode <= HTTP_UNAVAILABLE_END) {
            throw new HttpUnavailableException();
        } else if(statusCode != HTTP_OK) {
            throw new HttpErrorException(response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase());
        }

        HttpEntity entity = response.getEntity();

        if (entity != null) {
            InputStream contentStream = entity.getContent();
            try {
                return convertStreamToString(contentStream);
            } finally {
                contentStream.close();
            }
        }

        return null;
    }

    /**
     * Issue an HTTP GET for the given URL, return the response
     *
     * @param url url with url-encoded params
     * @return response, or null if there was no response
     * @throws IOException
     */
    public synchronized String get(String url) throws IOException {
        if(debug)
            Log.d("http-rest-client-get", url); //$NON-NLS-1$

        try {
            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = getClient().execute(httpGet);

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
        if(debug)
            Log.d("http-rest-client-post", url + " | " + data); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(data);
            HttpResponse response = getClient().execute(httpPost);

            return processHttpResponse(response);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
    }

}
