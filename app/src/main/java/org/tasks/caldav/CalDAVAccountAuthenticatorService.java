package org.tasks.caldav;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class CalDAVAccountAuthenticatorService extends Service {

    private CalDAVAccountAuthenticator authenticator;

    @Override
    public void onCreate() {
        authenticator = new CalDAVAccountAuthenticator(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return AccountManager.ACTION_AUTHENTICATOR_INTENT.equals(intent.getAction())
                ? authenticator.getIBinder()
                : null;
    }
}
