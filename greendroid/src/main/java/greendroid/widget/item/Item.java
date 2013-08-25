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
package greendroid.widget.item;

import greendroid.widget.itemview.ItemView;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ListView;

import com.cyrilmottier.android.greendroid.R;

/**
 * Base class for all items used in GreenDroid. An item represents a wrapper of
 * data. Each item contains at least all the information needed to display a
 * single row in a {@link ListView}.
 * 
 * @author Cyril Mottier
 */
public abstract class Item {

    private SparseArray<Object> mTags;
    private Object mTag;

    /**
     * Set to true when this item is enabled?
     */
    public boolean enabled;

    /**
     * Constructs a new item.
     */
    public Item() {
        // By default, an item is enabled
        enabled = true;
    }

    /**
     * Returns the tag associated to that item.
     * 
     * @return The tag associated to this item.
     */
    public Object getTag() {
        return mTag;
    }

    /**
     * Sets the tag associated with this item. A tag is often used to store
     * extra information.
     * 
     * @param tag The tag associated to this item
     */
    public void setTag(Object tag) {
        mTag = tag;
    }

    /**
     * Returns the tag associated with this item and the specified key.
     * 
     * @param key The key of the tag to retrieve
     * @return The tag associated to the key <em>key</em> or null if no tags are
     *         associated to that key
     */
    public Object getTag(int key) {
        return (mTags == null) ? null : mTags.get(key);
    }

    /**
     * Sets a tag associated with this item and a key. A tag is often used to
     * store extra information.
     * 
     * @param key The key for the specified tag
     * @param tag A tag that will be associated to that item
     */
    public void setTag(int key, Object tag) {
        if (mTags == null) {
            mTags = new SparseArray<Object>();
        }
        mTags.put(key, tag);
    }

    /**
     * Returns a view that is associated to the current item. The returned view
     * is normally capable of being a good recipient for all item's information.
     * 
     * @param context The context in which the {@link ItemView} will be used
     * @param parent The parent view of that new view. It is usually the parent
     *            ListView
     * @return A new allocated view for the current Item
     */
    public abstract ItemView newView(Context context, ViewGroup parent);

    /**
     * Helper method to inflate a layout using a given Context and a layoutID.
     * 
     * @param context The current context
     * @param layoutID The identifier of the layout to inflate
     * @return A newly inflated view
     */
    protected static ItemView createCellFromXml(Context context, int layoutID, ViewGroup parent) {
        return (ItemView) LayoutInflater.from(context).inflate(layoutID, parent, false);
    }

    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException,
            IOException {
        TypedArray a = r.obtainAttributes(attrs, R.styleable.Item);
        enabled = a.getBoolean(R.styleable.Item_enabled, enabled);
        a.recycle();
    }

}
