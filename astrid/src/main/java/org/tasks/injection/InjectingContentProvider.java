package org.tasks.injection;

import android.content.ContentProvider;
import android.content.Context;

public abstract class InjectingContentProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        Context context = getContext();
        Dagger.getObjectGraph(context)
                .plus(new ContentProviderModule())
                .inject(this);
        return true;
    }
}
