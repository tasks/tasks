/*
 * Copyright 2009 Google Inc.
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

import java.util.Iterator;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.googlelogin.GoogleLoginServiceConstants;
import com.google.android.googlelogin.GoogleLoginServiceHelper;

/**
 * AuthManager keeps track of the current auth token for a user. The advantage
 * over just passing around a String is that this class can renew the auth
 * token if necessary, and it will change for all classes using this
 * AuthManager.
 */
public class AuthManagerOld implements AuthManager {
  /** The activity that will handle auth result callbacks. */
  private final Activity activity;

  /** The code used to tell the activity that it is an auth result. */
  private final int code;

  /** Extras to pass into the getCredentials function. */
  private final Bundle extras;

  /** True if the account must be a Google account (not a domain account). */
  private final boolean requireGoogle;

  /** The name of the service to authorize for. */
  private final String service;

  /** A list of handlers to call when a new auth token is fetched. */
  private final Vector<Runnable> newTokenListeners = new Vector<Runnable>();

  /** The most recently fetched auth token or null if none is available. */
  private String authToken;

  /**
   * The number of handlers at the beginning of the above list that shouldn't
   * be removed after they are called.
   */
  private int stickyNewTokenListenerCount;

  /**
   * AuthManager requires many of the same parameters as
   * {@link GoogleLoginServiceHelper#getCredentials(Activity, int, Bundle,
   * boolean, String, boolean)}. The activity must have
   * a handler in {@link Activity#onActivityResult} that calls
   * {@link #authResult(int, Intent)} if the request code is the code given
   * here.
   *
   * @param activity An activity with a handler in
   *        {@link Activity#onActivityResult} that calls
   *        {@link #authResult(int, Intent)} when {@literal code} is the request
   *        code
   * @param code The request code to pass to
   *        {@link Activity#onActivityResult} when
   *        {@link #authResult(int, Intent)} should be called
   * @param extras A {@link Bundle} of extras for
   *        {@link GoogleLoginServiceHelper}
   * @param requireGoogle True if the account must be a Google account
   * @param service The name of the service to authenticate as
   */
  public AuthManagerOld(Activity activity, int code, Bundle extras,
      boolean requireGoogle, String service) {
    this.activity = activity;
    this.code = code;
    this.extras = extras;
    this.requireGoogle = requireGoogle;
    this.service = service;
  }

  /* (non-Javadoc)
   * @see com.google.android.apps.mytracks.io.AuthManager#doLogin(java.lang.Runnable)
   */
  public void doLogin(Runnable whenFinished, Object o) {
    synchronized (newTokenListeners) {
      if (whenFinished != null) {
        newTokenListeners.add(whenFinished);
      }
    }
    activity.runOnUiThread(new LoginRunnable());
  }

  /**
   * Runnable which actually gets login credentials.
   */
  private class LoginRunnable implements Runnable {
    @Override
    public void run() {

      GoogleLoginServiceHelper.getCredentials(
          activity, code, extras, requireGoogle, service, true);
    }
  }

  /* (non-Javadoc)
   * @see com.google.android.apps.mytracks.io.AuthManager#authResult(int, android.content.Intent)
   */
  public boolean authResult(int resultCode, Intent results) {
    if (resultCode == Activity.RESULT_OK) {
      authToken = results.getStringExtra(
          GoogleLoginServiceConstants.AUTHTOKEN_KEY);
      if (authToken == null) {
        GoogleLoginServiceHelper.getCredentials(
            activity, code, extras, requireGoogle, service, false);
        return true;
      } else {
        // Notify all active listeners that we have a new auth token.
        synchronized (newTokenListeners) {
          Iterator<Runnable> iter = newTokenListeners.iterator();
          while (iter.hasNext()) {
            iter.next().run();
          }
          iter = null;
          // Remove anything not in the sticky part of the list.
          newTokenListeners.setSize(stickyNewTokenListenerCount);
        }
        return true;
      }
    }
    return false;
  }

  /* (non-Javadoc)
   * @see com.google.android.apps.mytracks.io.AuthManager#getAuthToken()
   */
  public String getAuthToken() {
    return authToken;
  }

  /* (non-Javadoc)
   * @see com.google.android.apps.mytracks.io.AuthManager#invalidateAndRefresh(java.lang.Runnable)
   */
  public void invalidateAndRefresh(Runnable whenFinished) {
    synchronized (newTokenListeners) {
      if (whenFinished != null) {
        newTokenListeners.add(whenFinished);
      }
    }
    activity.runOnUiThread(new Runnable() {
      public void run() {
        GoogleLoginServiceHelper.invalidateAuthToken(activity, code, authToken);
      }
    });
  }

  /**
   * Adds a {@link Runnable} to be executed every time the auth token is
   * updated. The {@link Runnable} will not be removed until manually removed
   * with {@link #removeStickyNewTokenListener(Runnable)}.
   *
   * @param listener The {@link Runnable} to execute every time a new auth
   *        token is fetched
   */
  public void addStickyNewTokenListener(Runnable listener) {
    synchronized (newTokenListeners) {
      newTokenListeners.add(0, listener);
      stickyNewTokenListenerCount++;
    }
  }

  /**
   * Stops executing the given {@link Runnable} every time the auth token is
   * updated. This {@link Runnable} must have been added with
   * {@link #addStickyNewTokenListener(Runnable)} above. If the
   * {@link Runnable} was added more than once, only the first occurrence
   * will be removed.
   *
   * @param listener The {@link Runnable} to stop executing every time a new
   *        auth token is fetched
   */
  public void removeStickyNewTokenListener(Runnable listener) {
    synchronized (newTokenListeners) {
      if (stickyNewTokenListenerCount > 0
          && newTokenListeners.remove(listener)) {
        stickyNewTokenListenerCount--;
      }
    }
  }
}
