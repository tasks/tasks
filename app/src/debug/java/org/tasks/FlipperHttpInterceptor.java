package org.tasks;

import static com.todoroo.andlib.utility.DateUtilities.now;

import com.facebook.flipper.plugins.network.NetworkFlipperPlugin;
import com.facebook.flipper.plugins.network.NetworkReporter.Header;
import com.facebook.flipper.plugins.network.NetworkReporter.RequestInfo;
import com.facebook.flipper.plugins.network.NetworkReporter.ResponseInfo;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseInterceptor;
import com.google.api.client.json.GenericJson;
import com.todoroo.astrid.helper.UUIDHelper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

public class FlipperHttpInterceptor<T> implements HttpExecuteInterceptor, HttpResponseInterceptor {

  private final Class<T> responseClass;
  private final String requestId = UUIDHelper.newUUID();
  private final NetworkFlipperPlugin plugin;
  private T body;

  FlipperHttpInterceptor(NetworkFlipperPlugin plugin, Class<T> responseClass) {
    this.responseClass = responseClass;
    this.plugin = plugin;
  }

  @Override
  public void intercept(HttpRequest request) {
    plugin.reportRequest(toRequestInfo(request, now()));
  }

  @Override
  public void interceptResponse(HttpResponse response) throws IOException {
    plugin.reportResponse(toResponseInfo(response, now()));
  }

  public void report(HttpResponse response, long start, long end) throws IOException {
    plugin.reportRequest(toRequestInfo(response.getRequest(), start));
    plugin.reportResponse(toResponseInfo(response, end));
  }

  public T getResponse() {
    return body;
  }

  private RequestInfo toRequestInfo(HttpRequest request, long timestamp) {
    RequestInfo requestInfo = new RequestInfo();
    requestInfo.method = request.getRequestMethod();
    requestInfo.body = bodyToByteArray(request.getContent());
    requestInfo.headers = getHeaders(request.getHeaders());
    requestInfo.requestId = requestId;
    requestInfo.timeStamp = timestamp;
    requestInfo.uri = request.getUrl().toString();
    return requestInfo;
  }

  private ResponseInfo toResponseInfo(HttpResponse response, long timestamp) throws IOException {
    ResponseInfo responseInfo = new ResponseInfo();
    responseInfo.timeStamp = timestamp;
    responseInfo.headers = getHeaders(response.getHeaders());
    responseInfo.requestId = requestId;
    responseInfo.statusCode = response.getStatusCode();
    responseInfo.statusReason = response.getStatusMessage();
    body = response.parseAs(responseClass);
    if (body instanceof GenericJson) {
      try {
        responseInfo.body = ((GenericJson) body).toPrettyString().getBytes();
      } catch (IOException e) {
        Timber.e(e);
      }
    }
    return responseInfo;
  }

  private List<Header> getHeaders(HttpHeaders headers) {
    List<Header> result = new ArrayList<>();
    for (Map.Entry<String, Object> entry : headers.entrySet()) {
      result.add(new Header(entry.getKey(), entry.getValue().toString()));
    }
    return result;
  }

  private byte[] bodyToByteArray(HttpContent content) {
    if (content == null) {
      return null;
    }
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      content.writeTo(output);
    } catch (IOException e) {
      Timber.e(e);
      return null;
    }
    return output.toByteArray();
  }
}
