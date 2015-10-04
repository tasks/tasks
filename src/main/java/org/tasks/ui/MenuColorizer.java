/*
 * Copyright (C) 2014 Jared Rummler <jared.rummler@gmail.com>
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
package org.tasks.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

/**
 * Helper class to set the color and transparency for menu icons in an ActionBar/Toolbar.</p>
 *
 * Example usage:
 *
 * <pre>
 * <code>
 * public boolean onCreateOptionsMenu(Menu menu) {
 *     ...
 *     int color = getResources().getColor(R.color.your_awesome_color);
 *     int alpha = 204; // 80% alpha
 *     MenuColorizer.colorMenu(this, menu, color, alpha);
 *     ...
 * }
 * </code>
 * </pre>
 *
 * @author Jared Rummler <jared.rummler@gmail.com>
 * @since Dec 11, 2014
 */
public class MenuColorizer {

    private MenuColorizer() {
    }

    /** Sets a color filter on all menu icons, including the overflow button (if it exists) */
    public static void colorMenu(final Activity activity, final Menu menu, final int color) {
        colorMenu(activity, menu, color, 0);
    }

    /** Sets a color filter on all menu icons, including the overflow button (if it exists) */
    public static void colorMenu(final Activity activity, final Menu menu, final int color,
                                 final int alpha) {
        for (int i = 0, size = menu.size(); i < size; i++) {
            final MenuItem menuItem = menu.getItem(i);
            colorMenuItem(menuItem, color, alpha);
            if (menuItem.hasSubMenu()) {
                final SubMenu subMenu = menuItem.getSubMenu();
                for (int j = 0; j < subMenu.size(); j++) {
                    colorMenuItem(subMenu.getItem(j), color, alpha);
                }
            }
        }
        final View home = activity.findViewById(android.R.id.home);
        if (home != null) {
            home.post(new Runnable() {

                @Override
                public void run() {
                    colorOverflow(activity, color, alpha);
                }
            });
        }
    }

    /** Sets a color filter on a {@link MenuItem} */
    public static void colorMenuItem(final MenuItem menuItem, final int color) {
        colorMenuItem(menuItem, color, 0);
    }

    /** Sets a color filter on a {@link MenuItem} */
    public static void colorMenuItem(final MenuItem menuItem, final int color, final int alpha) {
        final Drawable drawable = menuItem.getIcon();
        if (drawable != null) {
            // If we don't mutate the drawable, then all drawable's with this id will have a color
            // filter applied to it.
            drawable.mutate();
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            if (alpha > 0) {
                drawable.setAlpha(alpha);
            }
        }
    }

    /** Sets a color filter on the OverflowMenuButton in an ActionBar or Toolbar */
    public static void colorOverflow(final Activity activity, final int color) {
        colorOverflow(activity, color, 0);
    }

    /** Sets a color filter on the OverflowMenuButton in an ActionBar or Toolbar */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static void colorOverflow(final Activity activity, final int color, final int alpha) {
        final ImageButton overflow = getOverflowMenu(activity);
        if (overflow != null) {
            overflow.setColorFilter(color);
            if (alpha > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    overflow.setImageAlpha(alpha);
                } else {
                    overflow.setAlpha(alpha);
                }
            }
        }
    }

    /* Find that OverflowMenuButton */
    private static ImageButton getOverflowMenu(final Activity activity,
                                               final ViewGroup... viewGroup) {
        final ViewGroup group;
        if (viewGroup == null || viewGroup.length == 0) {
            final int resId = activity.getResources().getIdentifier("action_bar", "id", "android");
            if (resId != 0) {
                group = (ViewGroup) activity.findViewById(resId);
            } else {
                group = (ViewGroup) activity.findViewById(android.R.id.content).getRootView();
            }
        } else {
            group = viewGroup[0];
        }
        ImageButton overflow = null;
        for (int i = 0, l = group.getChildCount(); i < l; i++) {
            final View v = group.getChildAt(i);
            if (v instanceof ImageButton
                    && v.getClass().getSimpleName().equals("OverflowMenuButton")) {
                overflow = (ImageButton) v;
            } else if (v instanceof ViewGroup) {
                overflow = getOverflowMenu(activity, (ViewGroup) v);
            }
            if (overflow != null) {
                break;
            }
        }
        return overflow;
    }

}
