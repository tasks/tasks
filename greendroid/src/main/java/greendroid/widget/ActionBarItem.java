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
package greendroid.widget;

import greendroid.graphics.drawable.ActionBarDrawable;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.cyrilmottier.android.greendroid.R;

/**
 * Base class representing an {@link ActionBarItem} used in {@link GDActionBar}s.
 * The base implementation exposes a single Drawable as well as a content
 * description.
 * 
 * @author Cyril Mottier
 */
public abstract class ActionBarItem {

    public enum Type {
        GoHome, // A house
        Search, // A magnifying glass
        Talk, // A speech bubble
        Compose, // A sheet of paper with a pen
        Export, // A dot with an arrow
        Share, // A dot with two arrows
        Refresh, // Two curved arrows
        TakePhoto, // A camera
        // PickPhoto, // Two pictures with an arrow
        Locate, // The traditional GMaps pin
        Edit, // A pencil
        Add, // A plus sign
        Star, // A star
        SortBySize, // Some bars
        LocateMyself, // A surrounded dot
        Compass,
        Help,
        Info,
        Settings,
        List,
        Trashcan,
        Eye,
        AllFriends,
        Group,
        Gallery,
        Slideshow,
        Mail
    }

    protected Drawable mDrawable;

    protected CharSequence mContentDescription;
    protected View mItemView;

    protected Context mContext;
    protected GDActionBar mActionBar;

    private int mItemId;

    void setActionBar(GDActionBar actionBar) {
        mContext = actionBar.getContext();
        mActionBar = actionBar;
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

    public ActionBarItem setDrawable(int drawableId) {
        return setDrawable(mContext.getResources().getDrawable(drawableId));
    }

    public ActionBarItem setDrawable(Drawable drawable) {
        if (drawable != mDrawable) {
            mDrawable = drawable;
            if (mItemView != null) {
                onDrawableChanged();
            }
        }
        return this;
    }

    public CharSequence getContentDescription() {
        return mContentDescription;
    }

    public ActionBarItem setContentDescription(int contentDescriptionId) {
        return setContentDescription(mContext.getString(contentDescriptionId));
    }

    public ActionBarItem setContentDescription(CharSequence contentDescription) {
        if (contentDescription != mContentDescription) {
            mContentDescription = contentDescription;
            if (mItemView != null) {
                onContentDescriptionChanged();
            }
        }
        return this;
    }

    public View getItemView() {
        if (mItemView == null) {
            mItemView = createItemView();
            prepareItemView();
        }
        return mItemView;
    }

    protected abstract View createItemView();

    protected void prepareItemView() {
    }

    protected void onDrawableChanged() {
    }

    protected void onContentDescriptionChanged() {
    }

    protected void onItemClicked() {
    }

    void setItemId(int itemId) {
        mItemId = itemId;
    }

    public int getItemId() {
        return mItemId;
    }

    static ActionBarItem createWithType(GDActionBar actionBar, ActionBarItem.Type type) {

        int drawableId = 0;
        int descriptionId = 0;

        switch (type) {
            case GoHome:
                drawableId = R.drawable.gd_action_bar_home;
                descriptionId = R.string.gd_go_home;
                break;

            case Search:
                drawableId = R.drawable.gd_action_bar_search;
                descriptionId = R.string.gd_search;
                break;

            case Talk:
                drawableId = R.drawable.gd_action_bar_talk;
                descriptionId = R.string.gd_talk;
                break;

            case Compose:
                drawableId = R.drawable.gd_action_bar_compose;
                descriptionId = R.string.gd_compose;
                break;

            case Export:
                drawableId = R.drawable.gd_action_bar_export;
                descriptionId = R.string.gd_export;
                break;

            case Share:
                drawableId = R.drawable.gd_action_bar_share;
                descriptionId = R.string.gd_share;
                break;

            case Refresh:
                return actionBar.newActionBarItem(LoaderActionBarItem.class)
                        .setDrawable(new ActionBarDrawable(actionBar.getResources(), R.drawable.gd_action_bar_refresh))
                        .setContentDescription(R.string.gd_refresh);

            case TakePhoto:
                drawableId = R.drawable.gd_action_bar_take_photo;
                descriptionId = R.string.gd_take_photo;
                break;
            //
            // case PickPhoto:
            // drawableId = R.drawable.gd_action_bar_pick_photo;
            // descriptionId = R.string.gd_pick_photo;
            // break;

            case Locate:
                drawableId = R.drawable.gd_action_bar_locate;
                descriptionId = R.string.gd_locate;
                break;

            case Edit:
                drawableId = R.drawable.gd_action_bar_edit;
                descriptionId = R.string.gd_edit;
                break;

            case Add:
                drawableId = R.drawable.gd_action_bar_add;
                descriptionId = R.string.gd_add;
                break;

            case Star:
                drawableId = R.drawable.gd_action_bar_star;
                descriptionId = R.string.gd_star;
                break;

            case SortBySize:
                drawableId = R.drawable.gd_action_bar_sort_by_size;
                descriptionId = R.string.gd_sort_by_size;
                break;

            case LocateMyself:
                drawableId = R.drawable.gd_action_bar_locate_myself;
                descriptionId = R.string.gd_locate_myself;
                break;
                
            case Compass:
            	drawableId = R.drawable.gd_action_bar_compass;
            	descriptionId = R.string.gd_compass;
            	break;
            	
            case Help:
            	drawableId = R.drawable.gd_action_bar_help;
            	descriptionId = R.string.gd_help;
            	break;

            case Info:
            	drawableId = R.drawable.gd_action_bar_info;
            	descriptionId = R.string.gd_info;
            	break;

            case Settings:
            	drawableId = R.drawable.gd_action_bar_settings;
            	descriptionId = R.string.gd_settings;
            	break;

            case List:
            	drawableId = R.drawable.gd_action_bar_list;
            	descriptionId = R.string.gd_list;
            	break;
            	
            case Trashcan:
            	drawableId = R.drawable.gd_action_bar_trashcan;
            	descriptionId = R.string.gd_trashcan;
            	break;

            case Eye:
            	drawableId = R.drawable.gd_action_bar_eye;
            	descriptionId = R.string.gd_eye;
            	break;

            case AllFriends:
            	drawableId = R.drawable.gd_action_bar_all_friends;
            	descriptionId = R.string.gd_all_friends;
            	break;

            case Group:
            	drawableId = R.drawable.gd_action_bar_group;
            	descriptionId = R.string.gd_group;
            	break;

            case Gallery:
            	drawableId = R.drawable.gd_action_bar_gallery;
            	descriptionId = R.string.gd_gallery;
            	break;

            case Slideshow:
            	drawableId = R.drawable.gd_action_bar_slideshow;
            	descriptionId = R.string.gd_slideshow;
            	break;

            case Mail:
            	drawableId = R.drawable.gd_action_bar_mail;
            	descriptionId = R.string.gd_mail;
            	break;

            default:
                // Do nothing but return null
                return null;
        }

        final Drawable d = new ActionBarDrawable(actionBar.getResources(), drawableId);

        return actionBar.newActionBarItem(NormalActionBarItem.class).setDrawable(d)
                .setContentDescription(descriptionId);
    }

}
