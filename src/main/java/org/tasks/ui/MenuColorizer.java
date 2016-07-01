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

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import org.tasks.R;

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

    public static void colorToolbar(Context context, Toolbar toolbar) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.actionBarPrimaryText, typedValue, true);
        colorToolbar(toolbar, typedValue.data);
    }

    public static void colorToolbar(Toolbar toolbar, int color) {
        Drawable wrapped = DrawableCompat.wrap(toolbar.getNavigationIcon());
        wrapped.setTint(color);
        toolbar.setNavigationIcon(wrapped);
        toolbar.setTitleTextColor(color);
        colorMenu(toolbar.getMenu(), color);
    }

    /** Sets a color filter on all menu icons, including the overflow button (if it exists) */
    private static void colorMenu(final Menu menu, final int color) {
        for (int i = 0, size = menu.size(); i < size; i++) {
            final MenuItem menuItem = menu.getItem(i);
            colorMenuItem(menuItem, color);
            if (menuItem.hasSubMenu()) {
                final SubMenu subMenu = menuItem.getSubMenu();
                for (int j = 0; j < subMenu.size(); j++) {
                    colorMenuItem(subMenu.getItem(j), color);
                }
            }
        }
    }

    /** Sets a color filter on a {@link MenuItem} */
    private static void colorMenuItem(final MenuItem menuItem, final int color) {
        final Drawable drawable = menuItem.getIcon();
        if (drawable != null) {
            // If we don't mutate the drawable, then all drawable's with this id will have a color
            // filter applied to it.
            drawable.mutate();
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }
}
