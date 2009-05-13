package com.timsu.astrid.sync;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.timsu.astrid.utilities.Preferences;

/**
 * SynchronizationService is the service that performs Astrid's background
 * synchronization with online task managers. Starting this service launches
 * the timer, which handles timing for synchronization.
 *
 * @author Tim Su
 *
 */
public class SynchronizationService extends Service {

    /** Service timer */
    private Timer timer = new Timer();

    /** Service activity */
    private static Context context;

    /** Set the activity for this service */
    public static void setContext(Context context) {
		SynchronizationService.context = context;
	}

    // --- utility methods

    public static void start() {
    	if(Preferences.getSyncAutoSyncFrequency(context) == null)
    		return;

    	Intent service = new Intent(context, SynchronizationService.class);
    	context.startService(service);
    }

    public static void stop() {
    	Intent service = new Intent(context, SynchronizationService.class);
    	context.stopService(service);
    }

    // ---

    @Override
    public IBinder onBind(Intent arg0) {
        return null; // unused
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // init the service here
        startService();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        shutdownService();
    }

    /** Start the timer that runs the service */
    private void startService() {
    	if(context == null)
    		return;

    	// figure out synchronization frequency
    	Integer syncFrequencySeconds = Preferences.getSyncAutoSyncFrequency(context);
    	if(syncFrequencySeconds == null) {
    		shutdownService();
    		return;
    	}

    	long interval = 1000L * syncFrequencySeconds;

    	// figure out last synchronize time
    	Date lastSyncDate = Preferences.getSyncLastSync(context);
    	Date lastAutoSyncDate = Preferences.getSyncLastSyncAttempt(context);

    	// if user never synchronized, give them a full offset period before bg sync
    	long latestSyncMillis = System.currentTimeMillis();
    	if(lastSyncDate != null)
    		latestSyncMillis = lastSyncDate.getTime();
    	if(lastAutoSyncDate != null && lastAutoSyncDate.getTime() > latestSyncMillis)
    		latestSyncMillis = lastAutoSyncDate.getTime();
    	long offset = 0;
    	if(latestSyncMillis != 0)
    		offset = Math.min(offset, Math.max(0, latestSyncMillis + interval -
    				System.currentTimeMillis()));

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
			public void run() {
                performSynchronization();
            }
        }, offset, interval);
        Log.i("astrid", "Synchronization Service Started, Offset: " + offset/1000 +
        		"s Interval: " + interval/1000);
    }

    /** Stop the timer that runs the service */
    private void shutdownService() {
        if (timer != null)
            timer.cancel();
        Log.i("astrid", "Synchronization Service Stopped");
    }

    /** Perform the actual synchronization */
    private void performSynchronization() {
    	if(context == null || context.getResources() == null)
    		return;

    	Log.i("astrid", "Automatic Synchronize Initiated.");
    	Preferences.setSyncLastSyncAttempt(context, new Date());

    	Synchronizer sync = new Synchronizer(true);
    	sync.synchronize(context, null);
    }
}
