package com.todoroo.astrid.gtasks.api;

import com.google.api.client.http.HttpResponseException;
import java.io.IOException;

public class HttpNotFoundException extends IOException {

  public HttpNotFoundException(HttpResponseException e) {
    super(e.getMessage());
  }
}
