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
    RequestInfo requestInfo = new RequestInfo();
    requestInfo.method = request.getRequestMethod();
    requestInfo.body = bodyToByteArray(request.getContent());
    requestInfo.headers = getHeaders(request.getHeaders());
    requestInfo.requestId = requestId;
    requestInfo.timeStamp = now();
    requestInfo.uri = request.getUrl().toString();
    plugin.reportRequest(requestInfo);
  }

  @Override
  public void interceptResponse(HttpResponse response) throws IOException {
    ResponseInfo responseInfo = new ResponseInfo();
    responseInfo.timeStamp = now();
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
    plugin.reportResponse(responseInfo);
  }

  public T getResponse() {
    return body;
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
