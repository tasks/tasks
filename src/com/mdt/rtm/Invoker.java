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
package com.mdt.rtm;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
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
public class Invoker {

    private static final String TAG = "rtm-invoker";

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

  public static final String REST_SERVICE_URL_POSTFIX = "/services/rest/";

  public static final String ENCODING = "UTF-8";

  public static final String API_SIG_PARAM = "api_sig";

  public static final long INVOCATION_INTERVAL = 300;

  private long lastInvocation;

  private final ApplicationInfo applicationInfo;

  private final MessageDigest digest;

  private String serviceRelativeUri;

  private HttpContext context;

  private BasicHttpParams globalHttpParams;

  private DefaultConnectionReuseStrategy connectionStrategy;

  private BasicHttpProcessor httpProcessor;

  private HttpClient httpClient;

  private DefaultHttpClientConnection connection;

  public Invoker(String serverHostName, int serverPortNumber, String serviceRelativeUri, ApplicationInfo applicationInfo)
      throws ServiceInternalException
  {
    this.serviceRelativeUri = serviceRelativeUri;
    new HttpHost(serverHostName, serverPortNumber);
    context = new BasicHttpContext();
    globalHttpParams = new BasicHttpParams();
    HttpProtocolParams.setVersion(globalHttpParams, HttpVersion.HTTP_1_1);
    HttpProtocolParams.setContentCharset(globalHttpParams, ENCODING);
    HttpProtocolParams.setUserAgent(globalHttpParams, "Jakarta-HttpComponents/1.1");
    HttpProtocolParams.setUseExpectContinue(globalHttpParams, true);
    connectionStrategy = new DefaultConnectionReuseStrategy();

    httpProcessor = new BasicHttpProcessor();
    // Required protocol interceptors
    httpProcessor.addInterceptor(new RequestContent());
    httpProcessor.addInterceptor(new RequestTargetHost());
    // Recommended protocol interceptors
    httpProcessor.addInterceptor(new RequestConnControl());
    httpProcessor.addInterceptor(new RequestUserAgent());
    httpProcessor.addInterceptor(new RequestExpectContinue());

    httpClient = new DefaultHttpClient();

    lastInvocation = System.currentTimeMillis();
    this.applicationInfo = applicationInfo;

    try
    {
      digest = MessageDigest.getInstance("md5");
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new ServiceInternalException("Could not create properly the MD5 digest", e);
    }
  }

  private StringBuffer computeRequestUri(Param... params)
      throws ServiceInternalException
  {
    final StringBuffer requestUri = new StringBuffer(serviceRelativeUri);
    if (params.length > 0)
    {
      requestUri.append("?");
    }
    for (Param param : params)
    {
      try
      {
        requestUri.append(param.getName()).append("=").append(URLEncoder.encode(param.getValue(), ENCODING)).append("&");
      }
      catch (Exception exception)
      {
        final StringBuffer message = new StringBuffer("Cannot encode properly the HTTP GET request URI: cannot execute query");
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
      throws ServiceException
  {
    long timeSinceLastInvocation = System.currentTimeMillis() - lastInvocation;
    if (timeSinceLastInvocation < INVOCATION_INTERVAL)
    {
      // In order not to invoke the RTM service too often
      try
      {
        Thread.sleep(INVOCATION_INTERVAL - timeSinceLastInvocation);
      }
      catch (InterruptedException e)
      {
        throw new ServiceInternalException("Unexpected interruption while attempting to pause for some time before invoking the RTM service back", e);
      }
    }

    Log.d(TAG, "Invoker running at " + new Date());

    // We prepare the network socket-based connection
    //prepareConnection();

    // We compute the URI
    final StringBuffer requestUri = computeRequestUri(params);
    HttpResponse response = null;

    final HttpGet request = new HttpGet("http://" + ServiceImpl.SERVER_HOST_NAME + requestUri.toString());
    request.setHeader(new BasicHeader(HTTP.CHARSET_PARAM, ENCODING));
    final String methodUri = request.getRequestLine().getUri();

    Element result;
    try
    {
      Log.i(TAG, "Executing the method:" + methodUri);
      response =  httpClient.execute(request);
          // httpExecutor.execute(request, connection, context);
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK)
      {
        Log.e(TAG, "Method failed: " + response.getStatusLine());

        // Tim: HTTP error. Let's wait a little bit
        if(!repeat) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                // ignore
            }
            return invoke(true, params);
        }

        throw new ServiceInternalException("method failed: " + response.getStatusLine());
      }

      // THINK: this method is deprecated, but the only way to get the body as a string, without consuming
      // the body input stream: the HttpMethodBase issues a warning but does not let you call the "setResponseStream()" method!
      final String responseBodyAsString = "";//EntityUtils.toString(response.getEntity());
      // Log.i(TAG, "  Invocation response:\r\n" + responseBodyAsString);
      final Document responseDoc = builder.parse(response.getEntity().getContent());
      final Element wrapperElt = responseDoc.getDocumentElement();
      if (!wrapperElt.getNodeName().equals("rsp"))
      {
        throw new ServiceInternalException("unexpected response returned by RTM service: " + responseBodyAsString);
      }
      else
      {
        String stat = wrapperElt.getAttribute("stat");
        if (stat.equals("fail"))
        {
          Node errElt = wrapperElt.getFirstChild();
          while (errElt != null && (errElt.getNodeType() != Node.ELEMENT_NODE || !errElt.getNodeName().equals("err")))
          {
            errElt = errElt.getNextSibling();
          }
          if (errElt == null)
          {
            throw new ServiceInternalException("unexpected response returned by RTM service: " + responseBodyAsString);
          }
          else
          {
            throw new ServiceException(Integer.parseInt(((Element) errElt).getAttribute("code")), ((Element) errElt).getAttribute("msg"));
          }
        }
        else
        {
          Node dataElt = wrapperElt.getFirstChild();
          while (dataElt != null && (dataElt.getNodeType() != Node.ELEMENT_NODE || dataElt.getNodeName().equals("transaction") == true))
          {
            try
            {
              Node nextSibling = dataElt.getNextSibling();
              if (nextSibling == null)
              {
                break;
              }
              else
              {
                dataElt = nextSibling;
              }
            }
            catch (IndexOutOfBoundsException exception)
            {
              // Some implementation may throw this exception, instead of returning a null sibling
              break;
            }
          }
          if (dataElt == null)
          {
            throw new ServiceInternalException("unexpected response returned by RTM service: " + responseBodyAsString);
          }
          else
          {
            result = (Element) dataElt;
          }
        }
      }
    }
    catch (IOException e)
    {
      throw new ServiceInternalException("Connection error", e);
    }
    catch (SAXException e)
    {
      throw new ServiceInternalException("XML Parse Exception", e);
    }
//    catch (HttpException e)
//    {
//      throw new ServiceInternalException("", e);
//    }
    finally
    {
      if (connection != null && (response == null || connectionStrategy.keepAlive(response, context) == false))
      {
        try
        {
          connection.close();
        }
        catch (IOException exception)
        {
          Log.w(TAG, new StringBuffer("Could not close properly the socket connection to '").append(connection.getRemoteAddress()).append("' on port ").append(
              connection.getRemotePort()).toString(), exception);
        }
      }
    }

    lastInvocation = System.currentTimeMillis();
    return result;
  }

  final String calcApiSig(Param... params)
      throws ServiceInternalException
  {
    try
    {
      digest.reset();
      digest.update(applicationInfo.getSharedSecret().getBytes(ENCODING));
      List<Param> sorted = Arrays.asList(params);
      Collections.sort(sorted);
      for (Param param : sorted)
      {
        digest.update(param.getName().getBytes(ENCODING));
        digest.update(param.getValue().getBytes(ENCODING));
      }
      return convertToHex(digest.digest());
    }
    catch (UnsupportedEncodingException e)
    {
      throw new ServiceInternalException("cannot hahdle properly the encoding", e);
    }
  }

  private static String convertToHex(byte[] data)
  {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < data.length; i++)
    {
      int halfbyte = (data[i] >>> 4) & 0x0F;
      int two_halfs = 0;
      do
      {
        if ((0 <= halfbyte) && (halfbyte <= 9))
          buf.append((char) ('0' + halfbyte));
        else
          buf.append((char) ('a' + (halfbyte - 10)));
        halfbyte = data[i] & 0x0F;
      }
      while (two_halfs++ < 1);
    }
    return buf.toString();
  }

}
