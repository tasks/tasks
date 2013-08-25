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
package greendroid.util;

import greendroid.app.GDApplication;
import greendroid.image.ImageCache;

import java.util.concurrent.ExecutorService;

import android.content.Context;

/**
 * Class that provides several utility methods related to GreenDroid.
 * 
 * @author Cyril Mottier
 */
public class GDUtils {

    private GDUtils() {
    }

    /**
     * Return the current {@link GDApplication}
     * 
     * @param context The calling context
     * @return The {@link GDApplication} the given context is linked to.
     */
    public static GDApplication getGDApplication(Context context) {
        return (GDApplication) context.getApplicationContext();
    }

    /**
     * Return the {@link GDApplication} image cache
     * 
     * @param context The calling context
     * @return The image cache of the current {@link GDApplication}
     */
    public static ImageCache getImageCache(Context context) {
        return getGDApplication(context).getImageCache();
    }

    /**
     * Return the {@link GDApplication} executors pool.
     * 
     * @param context The calling context
     * @return The executors pool of the current {@link GDApplication}
     */
    public static ExecutorService getExecutor(Context context) {
        return getGDApplication(context).getExecutor();
    }

}
