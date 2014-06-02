package org.tasks.injection;

import android.content.Context;

import com.todoroo.astrid.provider.Astrid2TaskProvider;
import com.todoroo.astrid.provider.Astrid3ContentProvider;
import com.todoroo.astrid.provider.SqlContentProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(injects = {
        Astrid2TaskProvider.class,
        Astrid3ContentProvider.class,
        SqlContentProvider.class
})
public class ContentProviderModule {
    private final Context context;

    public ContentProviderModule(Context context) {
        this.context = context;
    }

    @Singleton
    @Provides
    @ForApplication
    public Context getContext() {
        return context;
    }
}
