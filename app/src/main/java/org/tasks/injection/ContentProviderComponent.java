package org.tasks.injection;

import com.todoroo.astrid.provider.Astrid2TaskProvider;
import com.todoroo.astrid.provider.Astrid3ContentProvider;
import com.todoroo.astrid.provider.SqlContentProvider;

import dagger.Component;

@ApplicationScope
@Component(modules = {
        ApplicationModule.class,
        ContentProviderModule.class
})
public interface ContentProviderComponent {
    void inject(Astrid3ContentProvider astrid3ContentProvider);

    void inject(Astrid2TaskProvider astrid2TaskProvider);

    void inject(SqlContentProvider sqlContentProvider);
}
