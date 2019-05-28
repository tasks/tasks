package org.tasks;

import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import java.io.IOException;

public class ChainedHttpExecuteInterceptor implements HttpExecuteInterceptor {

  private final HttpExecuteInterceptor[] interceptors;

  ChainedHttpExecuteInterceptor(HttpExecuteInterceptor... interceptors) {
    this.interceptors = interceptors;
  }

  @Override
  public void intercept(HttpRequest request) throws IOException {
    for (HttpExecuteInterceptor interceptor : interceptors) {
      interceptor.intercept(request);
    }
  }
}
