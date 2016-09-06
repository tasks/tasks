package org.tasks.gtasks;

import android.content.Context;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.tasks.TasksScopes;
import com.todoroo.astrid.gtasks.api.HttpNotFoundException;

import java.io.IOException;

import timber.log.Timber;

public class GoogleTasksUnsuccessfulResponseHandler implements HttpUnsuccessfulResponseHandler {

    private static final BackOff BACKOFF = new ExponentialBackOff.Builder().build();

    private final Context context;
    private final GoogleAccountCredential googleAccountCredential;
    private final HttpBackOffUnsuccessfulResponseHandler backoffHandler = new HttpBackOffUnsuccessfulResponseHandler(BACKOFF);

    public GoogleTasksUnsuccessfulResponseHandler(Context context, GoogleAccountCredential googleAccountCredential) {
        this.context = context;
        this.googleAccountCredential = googleAccountCredential;
    }

    @Override
    public boolean handleResponse(HttpRequest request, HttpResponse response, boolean supportsRetry) throws IOException {
        HttpResponseException httpResponseException = new HttpResponseException(response);
        Timber.e(httpResponseException, httpResponseException.getMessage());
        if (!supportsRetry) {
            return false;
        }
        int statusCode = response.getStatusCode();
        if ((statusCode == HttpStatusCodes.STATUS_CODE_UNAUTHORIZED || statusCode == HttpStatusCodes.STATUS_CODE_FORBIDDEN)) {
            boolean shouldRetry = clearToken(googleAccountCredential);
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

    private boolean clearToken(GoogleAccountCredential credential){
        try {
            String token = credential.getToken();
            Timber.d("Invalidating %s", token);
            GoogleAuthUtil.clearToken(context, token);
            GoogleAuthUtil.getToken(context, credential.getSelectedAccount(), "oauth2:" + TasksScopes.TASKS, null);
            return true;
        } catch (GoogleAuthException e) {
            Timber.e(e, e.getMessage());
            return false;
        } catch (IOException e) {
            Timber.e(e, e.getMessage());
            return true;
        }
    }
}
