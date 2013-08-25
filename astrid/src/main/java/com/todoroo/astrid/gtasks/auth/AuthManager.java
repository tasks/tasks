/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.todoroo.astrid.gtasks.auth;

import android.content.Intent;

/**
 * This interface describes a class that will fetch and maintain a Google
 * authentication token.
 *
 * @author Sandor Dornbush
 */
public interface AuthManager {

  /**
   * Initializes the login process. The user should be asked to login if they
   * haven't already. The {@link Runnable} provided will be executed when the
   * auth token is successfully fetched.
   *
   * @param whenFinished A {@link Runnable} to execute when the auth token
   *        has been successfully fetched and is available via
   *        {@link #getAuthToken()}
   */
  public abstract void doLogin(Runnable whenFinished, Object o);

  /**
   * The {@link android.app.Activity} owner of this class should call this
   * function when it gets {@link android.app.Activity#onActivityResult} with
   * the request code passed into the constructor. The resultCode and results
   * should come directly from the {@link android.app.Activity#onActivityResult}
   * function. This function will return true if an auth token was successfully
   *  fetched or the process is not finished.
   *
   * @param resultCode The result code passed in to the
   *        {@link android.app.Activity}'s
   *        {@link android.app.Activity#onActivityResult} function
   * @param results The data passed in to the {@link android.app.Activity}'s
   *        {@link android.app.Activity#onActivityResult} function
   * @return True if the auth token was fetched or we aren't done fetching
   *         the auth token, or False if there was an error or the request was
   *         canceled
   */
  public abstract boolean authResult(int resultCode, Intent results);

  /**
   * Returns the current auth token. Response may be null if no valid auth
   * token has been fetched.
   *
   * @return The current auth token or null if no auth token has been
   *         fetched
   */
  public abstract String getAuthToken();

  /**
   * Invalidates the existing auth token and request a new one. The
   * {@link Runnable} provided will be executed when the new auth token is
   * successfully fetched.
   *
   * @param whenFinished A {@link Runnable} to execute when a new auth token
   *        is successfully fetched
   */
  public abstract void invalidateAndRefresh(Runnable whenFinished);

}
