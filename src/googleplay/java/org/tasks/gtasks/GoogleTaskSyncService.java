/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tasks.gtasks;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import timber.log.Timber;

/** Service to handle sync requests.
 *
 * <p>This service is invoked in response to Intents with action android.content.SyncAdapter, and
 * returns a Binder connection to SyncAdapter.
 *
 * <p>For performance, only one sync adapter will be initialized within this application's context.
 *
 * <p>Note: The SyncService itself is not notified when a new sync occurs. It's role is to
 * manage the lifecycle of our {@link GoogleTaskSyncAdapter} and provide a handle to said SyncAdapter to the
 * OS on request.
 */
public class GoogleTaskSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static GoogleTaskSyncAdapter sGoogleTaskSyncAdapter = null;

    /**
     * Thread-safe constructor, creates static {@link GoogleTaskSyncAdapter} instance.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("Service created");
        synchronized (sSyncAdapterLock) {
            if (sGoogleTaskSyncAdapter == null) {
                sGoogleTaskSyncAdapter = new GoogleTaskSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    /**
     * Logging-only destructor.
     */
    public void onDestroy() {
        super.onDestroy();
        Timber.d("Service destroyed");
    }

    /**
     * Return Binder handle for IPC communication with {@link GoogleTaskSyncAdapter}.
     *
     * <p>New sync requests will be sent directly to the SyncAdapter using this channel.
     *
     * @param intent Calling intent
     * @return Binder handle for {@link GoogleTaskSyncAdapter}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return sGoogleTaskSyncAdapter.getSyncAdapterBinder();
    }
}
