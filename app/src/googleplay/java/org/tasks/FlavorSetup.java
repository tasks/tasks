package org.tasks;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import javax.inject.Inject;
import org.tasks.billing.InventoryHelper;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.gtasks.PlayServices;
import org.tasks.jobs.JobManager;
import org.tasks.preferences.Preferences;

public class FlavorSetup {

  private final GtasksPreferenceService gtasksPreferenceService;
  private final InventoryHelper inventoryHelper;
  private final Preferences preferences;
  private final PlayServices playServices;
  private final GoogleAccountManager googleAccountManager;
  private final JobManager jobManager;

  @Inject
  public FlavorSetup(GtasksPreferenceService gtasksPreferenceService,
      InventoryHelper inventoryHelper,
      Preferences preferences, PlayServices playServices,
      GoogleAccountManager googleAccountManager, JobManager jobManager) {
    this.gtasksPreferenceService = gtasksPreferenceService;
    this.inventoryHelper = inventoryHelper;
    this.preferences = preferences;
    this.playServices = playServices;
    this.googleAccountManager = googleAccountManager;
    this.jobManager = jobManager;
  }

  public void setup() {
    inventoryHelper.initialize();
    gtasksPreferenceService.stopOngoing(); // if sync ongoing flag was set, clear it
    boolean backgroundSyncEnabled = preferences.getBoolean(R.string.p_background_sync, true);
    googleAccountManager.setBackgroundSynchronization(backgroundSyncEnabled);
    jobManager.setBackgroundSynchronization(backgroundSyncEnabled);
    playServices.refresh();
  }
}
