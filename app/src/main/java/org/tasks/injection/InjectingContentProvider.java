package org.tasks.injection;

import android.content.ContentProvider;
import android.content.Context;

public abstract class InjectingContentProvider extends ContentProvider {

  @Override
  public boolean onCreate() {
    Context context = getContext();
    inject(
        DaggerContentProviderComponent.builder()
            .applicationModule(new ApplicationModule(context.getApplicationContext()))
            .contentProviderModule(new ContentProviderModule())
            .build());

    return true;
  }

  protected abstract void inject(ContentProviderComponent component);
}
