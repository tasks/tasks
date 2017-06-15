package org.tasks;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.timber.StethoTree;
import com.squareup.leakcanary.LeakCanary;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;

import timber.log.Timber;

public class BuildSetup {
    private final Context context;

    @Inject
    public BuildSetup(@ForApplication Context context) {
        this.context = context;
    }

    public boolean setup() {
        Timber.plant(new Timber.DebugTree());
        Timber.plant(new StethoTree());
        Stetho.initializeWithDefaults(context);
        Application application = (Application) context.getApplicationContext();
        if (LeakCanary.isInAnalyzerProcess(context)) {
            return false;
        }
        LeakCanary.install(application);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
        return true;
    }
}
