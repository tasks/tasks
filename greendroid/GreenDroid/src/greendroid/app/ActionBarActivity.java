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

import greendroid.widget.ActionBar;
import greendroid.widget.ActionBarItem;
import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Defines all methods related to Activities embedding an {@link ActionBar}
 * 
 * @author Cyril Mottier
 */
public interface ActionBarActivity {

    /**
     * The optional title of the launched ActionBarActivity
     * 
     * @see Activity#setTitle(CharSequence)
     * @see Activity#setTitle(int)
     */
    static final String GD_ACTION_BAR_TITLE = "greendroid.app.ActionBarActivity.GD_ACTION_BAR_TITLE";

    /**
     * An integer that can be used to force the ActionBar to a particular
     * visibility. This is especially useful when using GDActivity inside a
     * GDTabActivity.
     * 
     * @see View#VISIBLE
     * @see View#INVISIBLE
     * @see View#GONE
     */
    static final String GD_ACTION_BAR_VISIBILITY = "greendroid.app.ActionBarActivity.GD_ACTION_BAR_VISIBILITY";

    /**
     * Clients may use this method to listen to {@link ActionBarItem}s clicks.
     * 
     * @param item The {@link ActionBarItem} that has been clicked
     * @param position The position of the clicked item. This number is equal or
     *            greater to zero. 0 is the leftmost item.
     * @return true if the method has handled the click on the
     *         {@link ActionBarItem} at position <em>position</em>. Otherwise it
     *         returns false.
     */
    boolean onHandleActionBarItemClick(ActionBarItem item, int position);

    /**
     * Returns the content view. Please note the content view is not the entire
     * view but a {@link FrameLayout} that contains everything but the
     * {@link ActionBar}.
     * 
     * @return The content view
     */
    FrameLayout getContentView();

    /**
     * Returns the {@link ActionBar}. Listening to {@link ActionBar} events
     * should be done via the
     * {@link ActionBarActivity#onHandleActionBarItemClick(ActionBarItem, int)}
     * method. Most of the time, this method don't need to be used directly.
     * 
     * @see {@link ActionBarActivity#onHandleActionBarItemClick(ActionBarItem, int)}
     * @see {@link ActionBarActivity#addActionBarItem(ActionBarItem)}
     * @see {@link ActionBarActivity#addActionBarItem(greendroid.widget.ActionBarItem.Type)}
     * @return The {@link ActionBar} currently displayed on screen
     */
    ActionBar getActionBar();

    /**
     * A simple utility method that casts the {@link Application} returned by
     * {@link #getApplication()} into a {@link GDApplication}
     * 
     * @return The current {@link GDApplication}
     */
    GDApplication getGDApplication();

    /**
     * Add a new item to the {@link ActionBar}.
     * 
     * @param item The item to add to the {@link ActionBar}
     */
    ActionBarItem addActionBarItem(ActionBarItem item);

    /**
     * Add a new item to the {@link ActionBar}.
     * 
     * @param item The item to add to the {@link ActionBar}
     * @param itemId Unique item ID. Use {@link ActionBar#NONE} if you do not
     *            need a unique ID.
     */
    ActionBarItem addActionBarItem(ActionBarItem item, int itemId);

    /**
     * Adds a new item of type <em>type</em> to the {@link ActionBar}.
     * 
     * @param actionBarItemType The item to add to the {@link ActionBar}
     */
    ActionBarItem addActionBarItem(ActionBarItem.Type actionBarItemType);

    /**
     * Adds a new item of type <em>type</em> to the {@link ActionBar}.
     * 
     * @param actionBarItemType The item to add to the {@link ActionBar}
     * @param itemId Unique item ID. Use {@link ActionBar#NONE} if you do not
     *            need a unique ID.
     */
    ActionBarItem addActionBarItem(ActionBarItem.Type actionBarItemType, int itemId);

    /**
     * Returns the identifier of the layout that needs to be created for this
     * {@link ActionBarActivity}
     * 
     * @return The layout identifier of the layout to create
     */
    int createLayout();

    /**
     * Called at the beginning of the {@link Activity#onContentChanged()}
     * method. This may be used to initialize all references on elements.
     */
    void onPreContentChanged();

    /**
     * Called at the end of the {@link Activity#onContentChanged()} method. This
     * may be use to initialize the content of the layout (titles, etc.)
     */
    void onPostContentChanged();
}
