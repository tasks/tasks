package org.tasks.gtasks;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import com.todoroo.astrid.gtasks.api.HttpNotFoundException;
import java.io.IOException;
import timber.log.Timber;

public class GoogleTasksUnsuccessfulResponseHandler implements HttpUnsuccessfulResponseHandler {

  private static final BackOff BACKOFF = new ExponentialBackOff.Builder().build();

  private final PlayServices playServices;
  private final GoogleAccountCredential googleAccountCredential;
  private final HttpBackOffUnsuccessfulResponseHandler backoffHandler =
      new HttpBackOffUnsuccessfulResponseHandler(BACKOFF);

  public GoogleTasksUnsuccessfulResponseHandler(
      PlayServices playServices, GoogleAccountCredential googleAccountCredential) {
    this.playServices = playServices;
    this.googleAccountCredential = googleAccountCredential;
  }

  @Override
  public boolean handleResponse(HttpRequest request, HttpResponse response, boolean supportsRetry)
      throws IOException {
    HttpResponseException httpResponseException = new HttpResponseException(response);
    Timber.e(httpResponseException);
    if (!supportsRetry) {
      return false;
    }
    int statusCode = response.getStatusCode();
    if ((statusCode == HttpStatusCodes.STATUS_CODE_UNAUTHORIZED
        || statusCode == HttpStatusCodes.STATUS_CODE_FORBIDDEN)) {
      boolean shouldRetry = playServices.clearToken(googleAccountCredential);
      if (!shouldRetry) {
        return false;
      }
    } else if (statusCode == 400) { // bad request
      throw httpResponseException;
    } else if (statusCode == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
      throw new HttpNotFoundException(httpResponseException);
    }

    return backoffHandler.handleResponse(request, response, supportsRetry);
  }
}
