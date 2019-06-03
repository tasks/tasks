package org.tasks;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import javax.inject.Inject;
import okhttp3.OkHttpClient;

public class DebugNetworkInterceptor {
  @Inject
  public DebugNetworkInterceptor() {}

  public void add(OkHttpClient.Builder builder) {}

  public <T> T execute(HttpRequest httpRequest, Class<T> responseClass) {
    return null;
  }

  public <T> T report(HttpResponse httpResponse, Class<T> responseClass, long start, long finish) {
    return null;
  }
}
