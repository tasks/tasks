/*
 * Copyright (C) 2010 Cyril Mottier (http://www.cyrilmottier.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package greendroid.app;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

/**
 * Defines various methods that should be overridden in order to style your
 * application.
 * 
 * @author Cyril Mottier
 */
public class GDApplication extends Application {

    /**
     * Returns the class of the home {@link Activity}. The home {@link Activity}
     * is the main entrance point of your application. This is usually where the
     * dashboard/general menu is displayed.
     * 
     * @return The Class of the home {@link Activity}
     */
    public Class<?> getHomeActivityClass() {
        return null;
    }

    /**
     * Each application may have an "application intent" which will be used when
     * the user clicked on the application button.
     * 
     * @return The main application {@link Intent} (may be null if you don't
     *         want to use the main application {@link Intent} feature)
     */
    public Intent getMainApplicationIntent() {
        return null;
    }
}
