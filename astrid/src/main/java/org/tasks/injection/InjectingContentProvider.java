package org.tasks.injection;

import android.content.ContentProvider;

public abstract class InjectingContentProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        ((Injector) getContext().getApplicationContext()).inject(this, new ContentProviderModule());

        return true;
    }
}
