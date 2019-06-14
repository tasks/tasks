package org.tasks.caldav;

import java.io.IOException;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import timber.log.Timber;

public class Response implements Function1<okhttp3.Response, Unit> {

  private final boolean parseBody;
  private okhttp3.Response response;
  private String body;

  public Response() {
    this(false);
  }

  public Response(boolean parseBody) {
    this.parseBody = parseBody;
  }

  @Override
  public Unit invoke(okhttp3.Response response) {
    this.response = response;
    if (parseBody) {
      try {
        body = response.body().string();
      } catch (IOException e) {
        Timber.e(e);
      }
    }
    return null;
  }

  public okhttp3.Response get() {
    return response;
  }

  public String getBody() {
    return body;
  }
}
