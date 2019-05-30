package org.tasks;

import android.content.Context;
import com.facebook.flipper.android.AndroidFlipperClient;
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor;
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import java.io.IOException;
import javax.inject.Inject;
import okhttp3.OkHttpClient;
import org.tasks.injection.ForApplication;

public class DebugNetworkInterceptor {

  private final Context context;

  @Inject
  public DebugNetworkInterceptor(@ForApplication Context context) {
    this.context = context;
  }

  private static NetworkFlipperPlugin getNetworkPlugin(Context context) {
    return AndroidFlipperClient.getInstance(context).getPlugin(NetworkFlipperPlugin.ID);
  }

  public void add(OkHttpClient.Builder builder) {
    builder.addNetworkInterceptor(new FlipperOkhttpInterceptor(getNetworkPlugin(context)));
  }

  public <T> T execute(HttpRequest request, Class<T> responseClass) throws IOException {
    FlipperHttpInterceptor<T> interceptor =
        new FlipperHttpInterceptor<>(getNetworkPlugin(context), responseClass);
    request
        .setInterceptor(new ChainedHttpExecuteInterceptor(request.getInterceptor(), interceptor))
        .setResponseInterceptor(interceptor)
        .execute();
    return interceptor.getResponse();
  }

  public <T> T report(HttpResponse httpResponse, Class<T> responseClass, long start, long finish)
      throws IOException {
    FlipperHttpInterceptor<T> interceptor =
        new FlipperHttpInterceptor<>(getNetworkPlugin(context), responseClass);
    interceptor.report(httpResponse, start, finish);
    return interceptor.getResponse();
  }
}
