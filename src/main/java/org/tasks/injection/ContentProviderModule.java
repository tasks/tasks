package org.tasks.injection;

import com.todoroo.astrid.provider.Astrid2TaskProvider;
import com.todoroo.astrid.provider.Astrid3ContentProvider;
import com.todoroo.astrid.provider.SqlContentProvider;

import dagger.Module;

@Module(addsTo = TasksModule.class,
        injects = {
        Astrid2TaskProvider.class,
        Astrid3ContentProvider.class,
        SqlContentProvider.class
})
public class ContentProviderModule {
}
