/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.utility.DateUtilities;
import javax.inject.Inject;
import org.tasks.preferences.Preferences;

/**
 * Methods for working with GTasks preferences
 *
 * @author timsu
 */
public class GtasksPreferenceService {

  private static final String IDENTIFIER = "gtasks"; //$NON-NLS-1$
  public static final String PREF_USER_NAME = IDENTIFIER + "_user"; //$NON-NLS-1$
  private static final String PREF_LAST_SYNC = "_last_sync"; //$NON-NLS-1$
  private static final String PREF_ONGOING = "_ongoing"; //$NON-NLS-1$
  private final Preferences preferences;

  @Inject
  public GtasksPreferenceService(Preferences preferences) {
    this.preferences = preferences;
  }

  public String getUserName() {
    return preferences.getStringValue(PREF_USER_NAME);
  }

  public void setUserName(String userName) {
    preferences.setString(PREF_USER_NAME, userName);
  }

  /**
   * @return Last Successful Sync Date, or 0
   */
  public long getLastSyncDate() {
    return preferences.getLong(IDENTIFIER + PREF_LAST_SYNC, 0);
  }

  /**
   * @return Last Error, or null if no last error
   */
  public boolean isOngoing() {
    return preferences.getBoolean(IDENTIFIER + PREF_ONGOING, false);
  }

  /**
   * Deletes Last Successful Sync Date
   */
  public void clearLastSyncDate() {
    preferences.clear(IDENTIFIER + PREF_LAST_SYNC);
  }

  /**
   * Set Ongoing
   */
  public void stopOngoing() {
    preferences.setBoolean(IDENTIFIER + PREF_ONGOING, false);
  }

  /**
   * Set Last Successful Sync Date
   */
  public void recordSuccessfulSync() {
    preferences.setLong(IDENTIFIER + PREF_LAST_SYNC, DateUtilities.now() + 1000);
  }

  /**
   * Set Last Attempted Sync Date
   */
  public void recordSyncStart() {
    preferences.setBoolean(IDENTIFIER + PREF_ONGOING, true);
  }
}
