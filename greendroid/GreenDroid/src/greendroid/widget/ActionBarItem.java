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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;

import com.cyrilmottier.android.greendroid.R;

/**
 * Base class representing an {@link ActionBarItem} used in {@link ActionBar}s.
 * The base implementation exposes a single Drawable as well as a
 * content description.
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
        LocateMyself
        // A surrounded dot
    }

    // Why, the hell, are those value protected to View ??? The simplest way to
    // get those sets here is to copy them. Another way would be to subclass
    // View just to access those protected fields ... yuck !!!
    private static final int[] ENABLE_STATE_SET = {
        android.R.attr.state_enabled
    };

    private static final int[] ENABLED_PRESSED_STATE_SET = {
            android.R.attr.state_enabled, android.R.attr.state_pressed
    };

    private static final int[] ENABLED_FOCUSED_STATE_SET = {
            android.R.attr.state_enabled, android.R.attr.state_focused
    };

    protected Drawable mDrawable;
    protected int mDrawableId;

    protected CharSequence mContentDescription;
    protected View mItemView;

    protected Context mContext;
    protected ActionBar mActionBar;

    void setActionBar(ActionBar actionBar) {
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

    static ActionBarItem createWithType(ActionBar actionBar, ActionBarItem.Type type) {

        int normalDrawableId;
        int altDrawableId;
        int descriptionId;

        switch (type) {
            case GoHome:
                normalDrawableId = R.drawable.gd_action_bar_home_normal;
                altDrawableId = R.drawable.gd_action_bar_home_alt;
                descriptionId = R.string.gd_go_home;
                break;

            case Search:
                normalDrawableId = R.drawable.gd_action_bar_search_normal;
                altDrawableId = R.drawable.gd_action_bar_search_alt;
                descriptionId = R.string.gd_search;
                break;

            case Talk:
                normalDrawableId = R.drawable.gd_action_bar_talk_normal;
                altDrawableId = R.drawable.gd_action_bar_talk_alt;
                descriptionId = R.string.gd_talk;
                break;

            case Compose:
                normalDrawableId = R.drawable.gd_action_bar_compose_normal;
                altDrawableId = R.drawable.gd_action_bar_compose_alt;
                descriptionId = R.string.gd_compose;
                break;

            case Export:
                normalDrawableId = R.drawable.gd_action_bar_export_normal;
                altDrawableId = R.drawable.gd_action_bar_export_alt;
                descriptionId = R.string.gd_export;
                break;

            case Share:
                normalDrawableId = R.drawable.gd_action_bar_share_normal;
                altDrawableId = R.drawable.gd_action_bar_share_alt;
                descriptionId = R.string.gd_share;
                break;

            case Refresh:
                return actionBar
                        .newActionBarItem(LoaderActionBarItem.class)
                        .setDrawable(
                                createStateListDrawable(actionBar.getContext(),
                                        R.drawable.gd_action_bar_refresh_normal, R.drawable.gd_action_bar_refresh_alt))
                        .setContentDescription(R.string.gd_refresh);

            case TakePhoto:
                normalDrawableId = R.drawable.gd_action_bar_take_photo_normal;
                altDrawableId = R.drawable.gd_action_bar_take_photo_alt;
                descriptionId = R.string.gd_take_photo;
                break;
            //
            // case PickPhoto:
            // normalDrawableId = R.drawable.gd_action_bar_pick_photo_normal;
            // altDrawableId = R.drawable.gd_action_bar_pick_photo_alt;
            // descriptionId = R.string.gd_pick_photo;
            // break;

            case Locate:
                normalDrawableId = R.drawable.gd_action_bar_locate_normal;
                altDrawableId = R.drawable.gd_action_bar_locate_alt;
                descriptionId = R.string.gd_locate;
                break;

            case Edit:
                normalDrawableId = R.drawable.gd_action_bar_edit_normal;
                altDrawableId = R.drawable.gd_action_bar_edit_alt;
                descriptionId = R.string.gd_edit;
                break;

            case Add:
                normalDrawableId = R.drawable.gd_action_bar_add_normal;
                altDrawableId = R.drawable.gd_action_bar_add_alt;
                descriptionId = R.string.gd_add;
                break;

            case Star:
                normalDrawableId = R.drawable.gd_action_bar_star_normal;
                altDrawableId = R.drawable.gd_action_bar_star_alt;
                descriptionId = R.string.gd_star;
                break;

            case SortBySize:
                normalDrawableId = R.drawable.gd_action_bar_sort_by_size_normal;
                altDrawableId = R.drawable.gd_action_bar_sort_by_size_alt;
                descriptionId = R.string.gd_sort_by_size;
                break;

            case LocateMyself:
                normalDrawableId = R.drawable.gd_action_bar_locate_myself_normal;
                altDrawableId = R.drawable.gd_action_bar_locate_myself_alt;
                descriptionId = R.string.gd_locate_myself;
                break;

            default:
                // Do nothing but return null
                return null;
        }

        final Drawable d = createStateListDrawable(actionBar.getContext(), normalDrawableId, altDrawableId);

        return actionBar.newActionBarItem(NormalActionBarItem.class).setDrawable(d)
                .setContentDescription(descriptionId);
    }

    private static Drawable createStateListDrawable(Context context, int normalDrawableId, int altDrawableId) {

        StateListDrawable stateListDrawable = new StateListDrawable();
        Drawable normalDrawable = context.getResources().getDrawable(normalDrawableId);
        Drawable altDrawable = context.getResources().getDrawable(altDrawableId);

        stateListDrawable.addState(ENABLED_FOCUSED_STATE_SET, altDrawable);
        stateListDrawable.addState(ENABLED_PRESSED_STATE_SET, altDrawable);
        stateListDrawable.addState(ENABLE_STATE_SET, normalDrawable);

        return stateListDrawable;
    }
}
