/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync;

public interface SyncResultCallback {

  /** Provider started sync */
  void started();

  /** Provider finished sync */
  void finished();
}
