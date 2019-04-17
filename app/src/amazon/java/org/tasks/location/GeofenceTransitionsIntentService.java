package org.tasks.location;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;

public class GeofenceTransitionsIntentService extends IntentService {

  public GeofenceTransitionsIntentService() {
    super("GeofenceTransitionsIntentService");
  }

  @Override
  protected void onHandleIntent(@Nullable Intent intent) {}

  public static class Broadcast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {}
  }
}
