/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import android.util.Log;

import com.todoroo.andlib.utility.AndroidUtilities;

/**
 * RestClient allows Android to consume web requests.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HttpRestClient implements RestClient {

    private static final int HTTP_UNAVAILABLE_END = 599;
    private static final int HTTP_UNAVAILABLE_START = 500;
    private static final int HTTP_OK = 200;

    private static final int TIMEOUT_MILLIS = 60000;

    private WeakReference<HttpClient> httpClient = null;

    protected boolean debug = false;

    private int timeout = TIMEOUT_MILLIS;

    @SuppressWarnings("nls")
    public HttpRestClient() {
        DependencyInjectionService.getInstance().inject(this);

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

        params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setSoTimeout(params, timeout);
        params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
        params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(30));
        params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

        cm = new ThreadSafeClientConnManager(params, schemeRegistry);
    }

    public HttpRestClient(int timeout) {
        super();
        this.timeout = timeout;

        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setSoTimeout(params, timeout);
    }

    private HttpParams params;
    private ThreadSafeClientConnManager cm;

    private synchronized HttpClient getClient() {
        if (httpClient == null || httpClient.get() == null) {
            DefaultHttpClient client = new DefaultHttpClient(cm, params);
            httpClient = new WeakReference<HttpClient>(client);
            actsAsGzippable(client);
            return client;
        }
        return httpClient.get();
    }

    @SuppressWarnings("nls")
    protected void actsAsGzippable(DefaultHttpClient client) {
        client.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(
                    final HttpRequest request,
                    final HttpContext context) throws HttpException, IOException {
                if (!request.containsHeader("Accept-Encoding"))
                        request.addHeader("Accept-Encoding", "gzip");
            }

        });

        client.addResponseInterceptor(new HttpResponseInterceptor() {
            public void process(
                    final HttpResponse response,
                    final HttpContext context) throws HttpException, IOException {
                HttpEntity entity = response.getEntity();
                Header ceheader = entity.getContentEncoding();
                if (ceheader != null) {
                    HeaderElement[] codecs = ceheader.getElements();
                    for (int i = 0; i < codecs.length; i++) {
                        if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                            response.setEntity(
                                    new GzipDecompressingEntity(response.getEntity()));
                            return;
                        }
                    }
                }
            }
        });
    }

    private static class GzipDecompressingEntity extends HttpEntityWrapper {

        public GzipDecompressingEntity(final HttpEntity entity) {
            super(entity);
        }

        @Override
        public InputStream getContent()
            throws IOException, IllegalStateException {

            // the wrapped entity's getContent() decides about repeatability
            InputStream wrappedin = wrappedEntity.getContent();

            return new GZIPInputStream(wrappedin);
        }

        @Override
        public long getContentLength() {
            // length of ungzipped content is not known
            return -1;
        }

    }

    private String processHttpResponse(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode >= HTTP_UNAVAILABLE_START && statusCode <= HTTP_UNAVAILABLE_END) {
            throw new HttpUnavailableException();
        }

        HttpEntity entity = response.getEntity();

        String body = null;
        if (entity != null) {
            InputStream contentStream = entity.getContent();
            try {
                body = AndroidUtilities.readInputStream(contentStream);
            } finally {
                contentStream.close();
            }
        }

        if(statusCode != HTTP_OK) {
            System.out.println(body);
            throw new HttpErrorException(response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase());
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
    public synchronized String post(String url, HttpEntity data, Header... headers) throws IOException {
        if(debug)
            Log.d("http-rest-client-post", url + " | " + data); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(data);
            for(Header header : headers)
                httpPost.addHeader(header);
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
