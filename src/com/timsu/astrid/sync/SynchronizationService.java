package com.timsu.astrid.sync;

import android.app.Activity;

/** A service that synchronizes with Astrid
 *
 * @author timsu
 *
 */
public interface SynchronizationService {

    /** Synchronize with the service */
    void synchronize(Activity activity);

    /** Called when synchronization with this service is turned off */
    void synchronizationDisabled(Activity activity);
}
