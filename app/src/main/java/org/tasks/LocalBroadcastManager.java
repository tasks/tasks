package org.tasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.todoroo.astrid.api.AstridApiConstants;
import javax.inject.Inject;
import org.tasks.injection.ForApplication;
import org.tasks.widget.AppWidgetManager;

public class LocalBroadcastManager {

  public static final String REFRESH = BuildConfig.APPLICATION_ID + ".REFRESH";
  public static final String REFRESH_LIST = BuildConfig.APPLICATION_ID + ".REFRESH_LIST";
  private static final String REPEAT = BuildConfig.APPLICATION_ID + ".REPEAT";
  private static final String REFRESH_PURCHASES = BuildConfig.APPLICATION_ID + ".REFRESH_PURCHASES";

  private final android.support.v4.content.LocalBroadcastManager localBroadcastManager;
  private final AppWidgetManager appWidgetManager;

  @Inject
  public LocalBroadcastManager(@ForApplication Context context, AppWidgetManager appWidgetManager) {
    this.appWidgetManager = appWidgetManager;
    localBroadcastManager = android.support.v4.content.LocalBroadcastManager.getInstance(context);
  }

  public void registerRefreshReceiver(BroadcastReceiver broadcastReceiver) {
    localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(REFRESH));
  }

  public void registerRefreshListReceiver(BroadcastReceiver broadcastReceiver) {
    localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(REFRESH_LIST));
  }

  public void registerRepeatReceiver(BroadcastReceiver broadcastReceiver) {
    localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(REPEAT));
  }

  public void registerPurchaseReceiver(BroadcastReceiver broadcastReceiver) {
    localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(REFRESH_PURCHASES));
  }

  public void broadcastRefresh() {
    localBroadcastManager.sendBroadcast(new Intent(REFRESH));
    appWidgetManager.updateWidgets();
  }

  public void broadcastRefreshList() {
    localBroadcastManager.sendBroadcast(new Intent(REFRESH_LIST));
  }

  /**
   * Action name for broadcast intent notifying that task was created from repeating template
   * <li>EXTRAS_TASK_ID id of the task
   * <li>EXTRAS_OLD_DUE_DATE task old due date (could be 0)
   * <li>EXTRAS_NEW_DUE_DATE task new due date (will not be 0)
   */
  public void broadcastRepeat(long id, long oldDueDate, long newDueDate) {
    Intent intent = new Intent(REPEAT);
    intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, id);
    intent.putExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, oldDueDate);
    intent.putExtra(AstridApiConstants.EXTRAS_NEW_DUE_DATE, newDueDate);
    localBroadcastManager.sendBroadcast(intent);
  }

  public void unregisterReceiver(BroadcastReceiver broadcastReceiver) {
    localBroadcastManager.unregisterReceiver(broadcastReceiver);
  }

  public void broadcastPurchasesUpdated() {
    localBroadcastManager.sendBroadcast(new Intent(REFRESH_PURCHASES));
  }
}
