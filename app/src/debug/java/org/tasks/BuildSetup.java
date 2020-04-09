package org.tasks;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import com.facebook.flipper.android.AndroidFlipperClient;
import com.facebook.flipper.android.utils.FlipperUtils;
import com.facebook.flipper.core.FlipperClient;
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin;
import com.facebook.flipper.plugins.inspector.DescriptorMapping;
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin;
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin;
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin;
import com.facebook.soloader.SoLoader;
import javax.inject.Inject;
import leakcanary.AppWatcher;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

class BuildSetup {

  private final Context context;
  private final Preferences preferences;

  @Inject
  BuildSetup(@ForApplication Context context, Preferences preferences) {
    this.context = context;
    this.preferences = preferences;
  }

  public boolean setup() {
    Timber.plant(new Timber.DebugTree());
    Application application = (Application) context.getApplicationContext();
    SoLoader.init(application, false);

    if (!preferences.getBoolean(R.string.p_leakcanary, false)) {
      AppWatcher.setConfig(AppWatcher.getConfig().newBuilder().enabled(false).build());
    }
    if (preferences.getBoolean(R.string.p_flipper, false) && FlipperUtils.shouldEnableFlipper(context)) {
      FlipperClient client = AndroidFlipperClient.getInstance(application);
      client.addPlugin(new InspectorFlipperPlugin(application, DescriptorMapping.withDefaults()));
      client.addPlugin(new DatabasesFlipperPlugin(application));
      client.addPlugin(new NetworkFlipperPlugin());
      client.addPlugin(new SharedPreferencesFlipperPlugin(application));
      client.start();
    }
    if (preferences.getBoolean(R.string.p_strict_mode_thread, false)) {
      StrictMode.setThreadPolicy(
          new StrictMode.ThreadPolicy.Builder()
              .detectDiskReads()
              .detectDiskWrites()
              .detectNetwork()
              .penaltyLog()
              .build());
    }
    if (preferences.getBoolean(R.string.p_strict_mode_vm, false)) {
      StrictMode.setVmPolicy(
          new StrictMode.VmPolicy.Builder()
              .detectLeakedSqlLiteObjects()
              .detectLeakedClosableObjects()
              .penaltyLog()
              .build());
    }
    return true;
  }
}
