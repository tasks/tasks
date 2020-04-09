package org.tasks.caldav;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

class Response implements Function1<okhttp3.Response, Unit> {

  private okhttp3.Response response;

  @Override
  public Unit invoke(okhttp3.Response response) {
    this.response = response;
    return null;
  }

  public okhttp3.Response get() {
    return response;
  }
}
