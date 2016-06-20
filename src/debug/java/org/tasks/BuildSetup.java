package org.tasks;

import android.app.Application;
import android.content.Context;

import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;

import timber.log.Timber;

public class BuildSetup {
    private Context context;

    @Inject
    public BuildSetup(@ForApplication Context context) {
        this.context = context;
    }

    public void setup() {
        Timber.plant(new Timber.DebugTree());
        Stetho.initializeWithDefaults(context);
        LeakCanary.install((Application) context);
    }
}
