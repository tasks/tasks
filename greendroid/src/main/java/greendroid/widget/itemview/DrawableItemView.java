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
package greendroid.widget.itemview;

import greendroid.widget.item.DrawableItem;
import greendroid.widget.item.Item;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cyrilmottier.android.greendroid.R;

public class DrawableItemView extends LinearLayout implements ItemView {

    private TextView mTextView;
    private ImageView mImageView;

    public DrawableItemView(Context context) {
        this(context, null);
    }

    public DrawableItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void prepareItemView() {
        mTextView = (TextView) findViewById(R.id.gd_text);
        mImageView = (ImageView) findViewById(R.id.gd_drawable);
    }

    public void setObject(Item object) {
        final DrawableItem item = (DrawableItem) object;
        mTextView.setText(item.text);

        final int drawableId = item.drawableId;
        if (drawableId == 0) {
            mImageView.setVisibility(View.GONE);
        } else {
            mImageView.setVisibility(View.VISIBLE);
            mImageView.setImageResource(drawableId);
        }
    }

}
