package org.tasks.injection;

import com.todoroo.astrid.provider.Astrid2TaskProvider;
import dagger.Component;

@ApplicationScope
@Component(modules = {ApplicationModule.class, ContentProviderModule.class})
public interface ContentProviderComponent {

  void inject(Astrid2TaskProvider astrid2TaskProvider);
}
