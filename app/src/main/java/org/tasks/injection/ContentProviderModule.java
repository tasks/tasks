package org.tasks.injection;

import dagger.Module;

@Module(includes = ProductionModule.class)
class ContentProviderModule {}
