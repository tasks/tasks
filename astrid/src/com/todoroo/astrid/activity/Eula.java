/*
 * Copyright 2008 Google Inc.
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
package com.todoroo.astrid.activity;

import android.app.Activity;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.service.TaskService;

/**
 * Displays an EULA ("End User License Agreement") that the user has to accept
 * before using the application. Your application should call
 * {@link Eula#showEula(android.app.Activity)} in the onCreate() method of the
 * first activity. If the user accepts the EULA, it will never be shown again.
 * If the user refuses, {@link android.app.Activity#finish()} is invoked on your
 * activity.
 */
public final class Eula {
    public static final String PREFERENCE_EULA_ACCEPTED = "eula.accepted"; //$NON-NLS-1$

    @Autowired
    TaskService taskService;

    private static void accept(Activity activity) {
        if (activity instanceof EulaCallback) {
            ((EulaCallback) activity).eulaAccepted();
        }
        Preferences.setBoolean(PREFERENCE_EULA_ACCEPTED, true);
    }

    private static void refuse(Activity activity) {
        if (activity instanceof EulaCallback) {
            ((EulaCallback) activity).eulaRefused();
        }
        activity.finish();
    }

    public static interface EulaCallback {
        public void eulaAccepted();

        public void eulaRefused();
    }

    private Eula() {
        // don't construct me
        DependencyInjectionService.getInstance().inject(this);
    }
}
