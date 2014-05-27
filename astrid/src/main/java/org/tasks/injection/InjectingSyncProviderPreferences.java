package org.tasks.injection;

import android.os.Bundle;

import com.todoroo.astrid.sync.SyncProviderPreferences;

public abstract class InjectingSyncProviderPreferences extends SyncProviderPreferences {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((Injector) getApplication()).inject(this, new ActivityModule());

        super.onCreate(savedInstanceState);
    }
}
