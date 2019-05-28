package org.tasks;

import com.google.api.client.http.HttpRequest;
import javax.inject.Inject;
import okhttp3.OkHttpClient;

public class DebugNetworkInterceptor {
  @Inject
  public DebugNetworkInterceptor() {}

  public void add(OkHttpClient.Builder builder) {}

  public <T> T execute(HttpRequest request, Class<T> responseClass) {
    return null;
  }
}
