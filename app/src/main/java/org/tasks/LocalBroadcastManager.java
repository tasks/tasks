package org.tasks;

import static com.google.common.collect.Lists.newArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.todoroo.astrid.api.AstridApiConstants;

import org.tasks.widget.AppWidgetManager;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class LocalBroadcastManager {

  public static final String REFRESH = BuildConfig.APPLICATION_ID + ".REFRESH";
  public static final String REFRESH_LIST = BuildConfig.APPLICATION_ID + ".REFRESH_LIST";
  private static final String TASK_COMPLETED = BuildConfig.APPLICATION_ID + ".REPEAT";
  private static final String REFRESH_PURCHASES = BuildConfig.APPLICATION_ID + ".REFRESH_PURCHASES";
  private static final String REFRESH_PREFERENCES = BuildConfig.APPLICATION_ID + ".REFRESH_PREFERENCES";

  private final androidx.localbroadcastmanager.content.LocalBroadcastManager localBroadcastManager;
  private final AppWidgetManager appWidgetManager;

  @Inject
  public LocalBroadcastManager(@ApplicationContext Context context, AppWidgetManager appWidgetManager) {
    this.appWidgetManager = appWidgetManager;
    localBroadcastManager =
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context);
  }

  public void registerRefreshReceiver(BroadcastReceiver broadcastReceiver) {
    localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(REFRESH));
  }

  public void registerRefreshListReceiver(BroadcastReceiver broadcastReceiver) {
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(REFRESH);
    intentFilter.addAction(REFRESH_LIST);
    localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter);
  }

  public void registerTaskCompletedReceiver(BroadcastReceiver broadcastReceiver) {
    localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(TASK_COMPLETED));
  }

  public void registerPurchaseReceiver(BroadcastReceiver broadcastReceiver) {
    localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(REFRESH_PURCHASES));
  }

  public void registerPreferenceReceiver(BroadcastReceiver broadcastReceiver) {
    localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(REFRESH_PREFERENCES));
  }

  public void broadcastRefresh() {
    localBroadcastManager.sendBroadcast(new Intent(REFRESH));
    appWidgetManager.updateWidgets();
  }

  public void broadcastRefreshList() {
    localBroadcastManager.sendBroadcast(new Intent(REFRESH_LIST));
  }

  public void broadcastPreferenceRefresh() {
    localBroadcastManager.sendBroadcast(new Intent(REFRESH_PREFERENCES));
  }

  public void broadcastTaskCompleted(long id, long oldDueDate) {
    broadcastTaskCompleted(newArrayList(id), oldDueDate);
  }

  public void broadcastTaskCompleted(ArrayList<Long> id) {
    broadcastTaskCompleted(id, 0);
  }

  private void broadcastTaskCompleted(ArrayList<Long> id, long oldDueDate) {
    Intent intent = new Intent(TASK_COMPLETED);
    intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, id);
    intent.putExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, oldDueDate);
    localBroadcastManager.sendBroadcast(intent);
  }

  public void unregisterReceiver(BroadcastReceiver broadcastReceiver) {
    localBroadcastManager.unregisterReceiver(broadcastReceiver);
  }

  public void broadcastPurchasesUpdated() {
    localBroadcastManager.sendBroadcast(new Intent(REFRESH_PURCHASES));
  }

  public void reconfigureWidget(int appWidgetId) {
    appWidgetManager.reconfigureWidgets(appWidgetId);
  }
}
