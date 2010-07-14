/*
 * Copyright 2007, MetaDimensional Technologies Inc.
 *
 *
 * This file is part of the RememberTheMilk Java API.
 *
 * The RememberTheMilk Java API is free software; you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * The RememberTheMilk Java API is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.todoroo.astrid.rmilk.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import android.util.Log;

/**
 * Handles the details of invoking a method on the RTM REST API.
 *
 * @author Will Ross Jun 21, 2007
 */
@SuppressWarnings("nls")
public class Invoker {

    private static final String TAG = "rtm-invoker"; //$NON-NLS-1$

  private static final DocumentBuilder builder;
  static
  {
    // Done this way because the builder is marked "final"
    DocumentBuilder aBuilder;
    try
    {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      factory.setValidating(false);
      aBuilder = factory.newDocumentBuilder();
    }
    catch (Exception exception)
    {
      Log.e(TAG, "Unable to construct a document builder", exception);
      aBuilder = null;
    }
    builder = aBuilder;
  }

  private static final String ENCODING = "UTF-8"; //$NON-NLS-1$

  private static final String API_SIG_PARAM = "api_sig"; //$NON-NLS-1$

  private static final long INVOCATION_INTERVAL = 400;

  private long lastInvocation;

  private final ApplicationInfo applicationInfo;

  private final MessageDigest digest;

  private String serviceRelativeUri;

  private HttpClient httpClient;

  public Invoker(@SuppressWarnings("unused") String serverHostName,
          @SuppressWarnings("unused") int serverPortNumber,
            String serviceRelativeUri, ApplicationInfo applicationInfo)
            throws ServiceInternalException {

        this.serviceRelativeUri = serviceRelativeUri;

        httpClient = new DefaultHttpClient();

        lastInvocation = System.currentTimeMillis();
        this.applicationInfo = applicationInfo;

        try {
            digest = MessageDigest.getInstance("md5"); //$NON-NLS-1$
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceInternalException(
                    "Could not create properly the MD5 digest", e);
        }
    }

  private StringBuffer computeRequestUri(Param... params)
            throws ServiceInternalException {
        final StringBuffer requestUri = new StringBuffer(serviceRelativeUri);
        if (params.length > 0) {
            requestUri.append("?");
        }
        for (Param param : params) {
            try {
                requestUri.append(param.getName()).append("=").append(
                        URLEncoder.encode(param.getValue(), ENCODING)).append(
                        "&");
            } catch (Exception exception) {
                final StringBuffer message = new StringBuffer(
                        "Cannot encode properly the HTTP GET request URI: cannot execute query");
                Log.e(TAG, message.toString(), exception);
                throw new ServiceInternalException(message.toString());
            }
        }
        requestUri.append(API_SIG_PARAM).append("=").append(calcApiSig(params));
        return requestUri;
    }

    /** Call invoke with a false repeat */
    public Element invoke(Param... params) throws ServiceException {
        return invoke(false, params);
    }

  public Element invoke(boolean repeat, Param... params)
            throws ServiceException {
        long timeSinceLastInvocation = System.currentTimeMillis() -
            lastInvocation;
        if (timeSinceLastInvocation < INVOCATION_INTERVAL) {
            // In order not to invoke the RTM service too often
            try {
                Thread.sleep(INVOCATION_INTERVAL - timeSinceLastInvocation);
            } catch (InterruptedException e) {
                return null;
            }
        }

        // We compute the URI
        final StringBuffer requestUri = computeRequestUri(params);
        HttpResponse response = null;

        final HttpGet request = new HttpGet("http://" //$NON-NLS-1$
            + ServiceImpl.SERVER_HOST_NAME + requestUri.toString());
        final String methodUri = request.getRequestLine().getUri();

        Element result;
        try {
            Log.i(TAG, "Executing the method:" + methodUri); //$NON-NLS-1$
            response = httpClient.execute(request);
            lastInvocation = System.currentTimeMillis();

            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.e(TAG, "Method failed: " + response.getStatusLine()); //$NON-NLS-1$

                // Tim: HTTP error. Let's wait a little bit
                if (!repeat) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    return invoke(true, params);
                }

                throw new ServiceInternalException("method failed: "
                    + response.getStatusLine());
            }

            final Document responseDoc = builder.parse(response.getEntity()
                    .getContent());
            final Element wrapperElt = responseDoc.getDocumentElement();
            if (!wrapperElt.getNodeName().equals("rsp")) {
                throw new ServiceInternalException(
                        "unexpected response returned by RTM service: "
                            + wrapperElt.getNodeName());
            } else {
                String stat = wrapperElt.getAttribute("stat");
                if (stat.equals("fail")) {
                    Node errElt = wrapperElt.getFirstChild();
                    while (errElt != null
                        && (errElt.getNodeType() != Node.ELEMENT_NODE || !errElt
                                .getNodeName().equals("err"))) {
                        errElt = errElt.getNextSibling();
                    }
                    if (errElt == null) {
                        throw new ServiceInternalException(
                                "unexpected response returned by RTM service: "
                                    + wrapperElt.getNodeValue());
                    } else {
                        throw new ServiceException(Integer
                                .parseInt(((Element) errElt)
                                        .getAttribute("code")),
                                ((Element) errElt).getAttribute("msg"));
                    }
                } else {
                    Node dataElt = wrapperElt.getFirstChild();
                    while (dataElt != null
                        && (dataElt.getNodeType() != Node.ELEMENT_NODE || dataElt
                                .getNodeName().equals("transaction") == true)) {
                        try {
                            Node nextSibling = dataElt.getNextSibling();
                            if (nextSibling == null) {
                                break;
                            } else {
                                dataElt = nextSibling;
                            }
                        } catch (IndexOutOfBoundsException exception) {
                            // Some implementation may throw this exception,
                            // instead of returning a null sibling
                            break;
                        }
                    }
                    if (dataElt == null) {
                        throw new ServiceInternalException(
                                "unexpected response returned by RTM service: "
                                    + wrapperElt.getNodeValue());
                    } else {
                        result = (Element) dataElt;
                    }
                }
            }
        } catch (IOException e) {
            throw new ServiceInternalException("Error making connection: " +
                    e.getMessage(), e);
        } catch (SAXException e) {
            // repeat call if possible.
            if(!repeat)
                return invoke(true, params);
            else
                throw new ServiceInternalException("Error parsing response. " +
                		"Please try sync again!", e);
        } finally {
            httpClient.getConnectionManager().closeExpiredConnections();
        }

        return result;
    }

    final String calcApiSig(Param... params) throws ServiceInternalException {
        try {
            digest.reset();
            digest.update(applicationInfo.getSharedSecret().getBytes(ENCODING));
            List<Param> sorted = Arrays.asList(params);
            Collections.sort(sorted);
            for (Param param : sorted) {
                digest.update(param.getName().getBytes(ENCODING));
                digest.update(param.getValue().getBytes(ENCODING));
            }
            return convertToHex(digest.digest());
        } catch (UnsupportedEncodingException e) {
            throw new ServiceInternalException(
                    "cannot hahdle properly the encoding", e);
        }
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

}
