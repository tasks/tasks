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
import com.squareup.leakcanary.LeakCanary;
import javax.inject.Inject;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class BuildSetup {

  private final Context context;
  private final Preferences preferences;

  @Inject
  public BuildSetup(@ForApplication Context context, Preferences preferences) {
    this.context = context;
    this.preferences = preferences;
  }

  public boolean setup() {
    Timber.plant(new Timber.DebugTree());
    if (preferences.getBoolean(R.string.p_flipper, false) && FlipperUtils.shouldEnableFlipper(context)) {
      SoLoader.init(context, false);
      FlipperClient client = AndroidFlipperClient.getInstance(context);
      client.addPlugin(new InspectorFlipperPlugin(context, DescriptorMapping.withDefaults()));
      client.addPlugin(new DatabasesFlipperPlugin(context));
      client.addPlugin(new NetworkFlipperPlugin());
      client.addPlugin(new SharedPreferencesFlipperPlugin(context));
      client.start();
    }
    Application application = (Application) context.getApplicationContext();
    if (LeakCanary.isInAnalyzerProcess(context)) {
      return false;
    }
    if (preferences.getBoolean(R.string.p_leak_canary, false)) {
      LeakCanary.install(application);
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
