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

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

import com.cyrilmottier.android.greendroid.R;

/**
 * Default implementation of an {@link ActionBarItem}. A
 * {@link NormalActionBarItem} is a simple {@link ActionBarItem} containing a
 * single icon.
 * 
 * @author Cyril Mottier
 */
public class NormalActionBarItem extends ActionBarItem {

    @Override
    protected View createItemView() {
        return LayoutInflater.from(mContext).inflate(R.layout.gd_action_bar_item_base, mActionBar, false);
    }

    @Override
    protected void prepareItemView() {
        super.prepareItemView();
        final ImageButton imageButton = (ImageButton) mItemView.findViewById(R.id.gd_action_bar_item);
        imageButton.setImageDrawable(mDrawable);
        imageButton.setContentDescription(mContentDescription);
    }

    @Override
    protected void onContentDescriptionChanged() {
        super.onContentDescriptionChanged();
        mItemView.findViewById(R.id.gd_action_bar_item).setContentDescription(mContentDescription);
    }

    @Override
    protected void onDrawableChanged() {
        super.onDrawableChanged();
        ImageButton imageButton = (ImageButton) mItemView.findViewById(R.id.gd_action_bar_item);
        imageButton.setImageDrawable(mDrawable);
    }

}
