package org.tasks.injection;

import android.content.AbstractThreadedSyncAdapter;
import android.content.Context;

public abstract class InjectingAbstractThreadedSyncAdapter extends AbstractThreadedSyncAdapter {

    protected InjectingAbstractThreadedSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        inject(context);
    }

    private void inject(Context context) {
        inject(((InjectingApplication) context.getApplicationContext())
                .getComponent()
                .plus(new SyncAdapterModule()));
    }

    protected abstract void inject(SyncAdapterComponent component);
}
