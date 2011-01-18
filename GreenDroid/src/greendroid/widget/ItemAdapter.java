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

import greendroid.widget.item.DescriptionItem;
import greendroid.widget.item.DrawableItem;
import greendroid.widget.item.Item;
import greendroid.widget.item.LongTextItem;
import greendroid.widget.item.ProgressItem;
import greendroid.widget.item.SeparatorItem;
import greendroid.widget.item.SubtextItem;
import greendroid.widget.item.SubtitleItem;
import greendroid.widget.item.TextItem;
import greendroid.widget.item.ThumbnailItem;
import greendroid.widget.itemview.ItemView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * <p>
 * A {@link ListAdapter} that acts like an {@link ArrayAdapter}. It manages a
 * ListView that is backed by an array of {@link Item}s. This is more advanced
 * than a simple {@link ArrayAdapter} because it handles different types of
 * cells internally. Adding, removing items from the internal array is also
 * possible.
 * </p>
 * <p>
 * The {@link ListView} can be notified manually using
 * {@link notifyDataSetChanged} or automatically using the notifyOnChange flag.
 * </p>
 * <p>
 * Finally, an ItemAdapter can be created via XML code using the createFromXml
 * method. This is a very powerful feature when you want to display static data
 * or if you want to prepopulate your ItemAdapter.
 * </p>
 * 
 * @author Cyril Mottier
 */
public class ItemAdapter extends BaseAdapter {

    private static final int DEFAULT_MAX_VIEW_TYPE_COUNT = 10;

    private static class TypeInfo {
        int count;
        int type;
    }

    private List<Item> mItems;
    private HashMap<Class<? extends Item>, TypeInfo> mTypes;
    private Context mContext;

    private boolean mNotifyOnChange;
    private int mMaxViewTypeCount;

    /**
     * Constructs an empty ItemAdapter.
     * 
     * @param context The context associated with this array adapter.
     */
    public ItemAdapter(Context context) {
        this(context, new ArrayList<Item>());
    }

    /**
     * Constructs an ItemAdapter using the specified items.
     * <p>
     * <em>Note</em> : Using this constructor implies the internal array will be
     * immutable. As a result, adding or removing items will result in an
     * exception.
     * </p>
     * 
     * @param context The context associated with this array adapter.
     * @param items The array of Items use as underlying data for this
     *            ItemAdapter
     */
    public ItemAdapter(Context context, Item[] items) {
        this(context, Arrays.asList(items), DEFAULT_MAX_VIEW_TYPE_COUNT);
    }

    /**
     * Constructs an ItemAdapter using the specified items.
     * <p>
     * 
     * @param context The context associated with this array adapter.
     * @param items The list of Items used as data for this ItemAdapter
     */
    public ItemAdapter(Context context, List<Item> items) {
        this(context, items, DEFAULT_MAX_VIEW_TYPE_COUNT);
    }

    /**
     * Constructs an ItemAdapter using the specified items.
     * <p>
     * <em>Note</em> : Using this constructor implies the internal array will be
     * immutable. As a result, adding or removing items will result in an
     * exception.
     * </p>
     * <p>
     * <em><strong>Note:</strong> A ListAdapter doesn't handle variable view type 
     * count (even after a notifyDataSetChanged). An ItemAdapter handles several 
     * types of cell are therefore use a trick to overcome the previous problem.
     * This trick is to fool the ListView several types exist. If you already
     * know the number of item types you can have, simply set it using this method</em>
     * </p>
     * 
     * @param context The context associated with this array adapter.
     * @param items The array of Items use as underlying data for this
     *            ItemAdapter
     * @param maxViewTypeCount The maximum number of view type that may be
     *            generated by this ItemAdapter
     */
    public ItemAdapter(Context context, Item[] items, int maxViewTypeCount) {
        this(context, Arrays.asList(items), maxViewTypeCount);
    }

    /**
     * Constructs an ItemAdapter using the specified items.
     * <p>
     * <em><strong>Note:</strong> A ListAdapter doesn't handle variable view type 
     * count (even after a notifyDataSetChanged). An ItemAdapter handles several 
     * types of cell are therefore use a trick to overcome the previous problem.
     * This trick is to fool the ListView several types exist. If you already
     * know the number of item types you can have, simply set it using this method</em>
     * </p>
     * 
     * @param context The context associated with this array adapter.
     * @param items The list of Items used as data for this ItemAdapter
     * @param maxViewTypeCount The maximum number of view type that may be
     *            generated by this ItemAdapter
     */
    public ItemAdapter(Context context, List<Item> items, int maxViewTypeCount) {
        mContext = context;
        mItems = items;
        mTypes = new HashMap<Class<? extends Item>, TypeInfo>();
        mMaxViewTypeCount = Integer.MAX_VALUE;

        for (Item item : mItems) {
            addItem(item);
        }

        mMaxViewTypeCount = Math.max(1, Math.max(mTypes.size(), maxViewTypeCount));
    }

    private void addItem(Item item) {
        final Class<? extends Item> klass = item.getClass();
        TypeInfo info = mTypes.get(klass);

        if (info == null) {
            final int type = mTypes.size();
            if (type >= mMaxViewTypeCount) {
                throw new RuntimeException("This ItemAdapter may handle only " + mMaxViewTypeCount
                        + " different view types.");
            }
            final TypeInfo newInfo = new TypeInfo();
            newInfo.count = 1;
            newInfo.type = type;
            mTypes.put(klass, newInfo);
        } else {
            info.count++;
        }
    }

    private void removeItem(Item item) {
        final Class<? extends Item> klass = item.getClass();
        TypeInfo info = mTypes.get(klass);

        if (info != null) {
            info.count--;
            if (info.count == 0) {
                // TODO cyril: Creating a pool to keep all TypeInfo instances
                // could be a great idea in the future.
                mTypes.remove(klass);
            }
        }
    }

    /**
     * Returns the context associated with this array adapter. The context is
     * used to create views from the resource passed to the constructor.
     * 
     * @return The Context associated to this ItemAdapter
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Returns the current number of different views types used in this
     * ItemAdapter. Having a <em>getCurrentViewTypeCount</em> equal to
     * <em>getViewTypeCount</em> means you won't be able to add a new type of
     * view in this adapter (The Adapter class doesn't allow variable view type
     * count).
     * 
     * @return The current number of different view types
     */
    public int getActualViewTypeCount() {
        return mTypes.size();
    }

    /**
     * Adds the specified object at the end of the array.
     * 
     * @param object The object to add at the end of the array.
     */
    public void add(Item item) {
        mItems.add(item);
        addItem(item);
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    /**
     * Inserts the specified object at the specified index in the array.
     * 
     * @param item The object to insert into the array.
     * @param index The index at which the object must be inserted.
     */
    public void insert(Item item, int index) {
        mItems.add(index, item);
        addItem(item);
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    /**
     * Removes the specified object from the array.
     * 
     * @param object The object to remove.
     */
    public void remove(Item item) {
        if (mItems.remove(item)) {
            removeItem(item);
            if (mNotifyOnChange) {
                notifyDataSetChanged();
            }
        }
    }

    /**
     * Remove all elements from the list.
     */
    public void clear() {
        mItems.clear();
        mTypes.clear();
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    /**
     * Sorts the content of this adapter using the specified comparator.
     * 
     * @param comparator The comparator used to sort the objects contained in
     *            this adapter.
     */
    public void sort(Comparator<? super Item> comparator) {
        Collections.sort(mItems, comparator);
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    /**
     * Control whether methods that change the list ({@link #add},
     * {@link #insert}, {@link #remove}, {@link #clear}) automatically call
     * {@link #notifyDataSetChanged}. If set to false, caller must manually call
     * notifyDataSetChanged() to have the changes reflected in the attached
     * view. The default is true, and calling notifyDataSetChanged() resets the
     * flag to true.
     * 
     * @param notifyOnChange if true, modifications to the list will
     *            automatically call {@link #notifyDataSetChanged}
     */
    public void setNotifyOnChange(boolean notifyOnChange) {
        mNotifyOnChange = notifyOnChange;
    }

    /**
     * Creates an ItemAdapter from a given resource ID
     * 
     * @param context The Context in which the ItemAdapter will be used in
     * @param xmlId The resource ID of an XML file that describes a set of
     *            {@link Item}
     * @return a new ItemAdapter constructed with the content of the file
     *         pointed by <em>xmlId</em>
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static ItemAdapter createFromXml(Context context, int xmlId) throws XmlPullParserException, IOException {
        return createFromXml(context, context.getResources().getXml(xmlId));
    }

    /**
     * Creates an ItemAdapter from a given XML document. Called on a parser
     * positioned at a tag in an XML document, tries to create an ItemAdapter
     * from that tag.
     * 
     * @param context The Context in which the ItemAdapter will be used in
     * @param xmlId The resource ID of an XML file that describes a set of
     *            {@link Item}
     * @return a new ItemAdapter constructed with the content of the file
     *         pointed by <em>xmlId</em>
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static ItemAdapter createFromXml(Context context, XmlPullParser parser) throws XmlPullParserException,
            IOException {
        AttributeSet attrs = Xml.asAttributeSet(parser);

        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals("item-array")) {
            throw new XmlPullParserException("Unknown start tag. Should be 'item-array'");
        }

        final List<Item> items = new ArrayList<Item>();
        final int innerDepth = parser.getDepth() + 1;
        final Resources r = context.getResources();

        int depth;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth || type != XmlPullParser.END_TAG)) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            if (depth > innerDepth) {
                continue;
            }

            String name = parser.getName();
            Item item;
            if (name.equals("text-item")) {
                item = new TextItem();
            } else if (name.equals("longtext-item")) {
                item = new LongTextItem();
            } else if (name.equals("description-item")) {
                item = new DescriptionItem();
            } else if (name.equals("separator-item")) {
                item = new SeparatorItem();
            } else if (name.equals("progress-item")) {
                item = new ProgressItem();
            } else if (name.equals("drawable-item")) {
                item = new DrawableItem();
            } else if (name.equals("subtitle-item")) {
                item = new SubtitleItem();
            } else if (name.equals("subtext-item")) {
                item = new SubtextItem();
            } else if (name.equals("thumbnail-item")) {
                item = new ThumbnailItem();
            } else {
                // TODO cyril: Remove that so that we can extend from
                // ItemAdapter and creates our own items via XML?
                throw new XmlPullParserException(parser.getPositionDescription() + ": invalid item tag " + name);
            }

            // TODO cyril: Here we should call a method that children may
            // override to be able to create our own Items

            if (item != null) {
                item.inflate(r, parser, attrs);
                items.add(item);
            }
        }

        return new ItemAdapter(context, items);
    }

    public int getCount() {
        return mItems.size();
    }

    public Object getItem(int position) {
        return mItems.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return mTypes.get(getItem(position).getClass()).type;
    }

    @Override
    public boolean isEnabled(int position) {
        return ((Item) getItem(position)).enabled;
    }

    @Override
    public int getViewTypeCount() {
        return mMaxViewTypeCount;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        final Item item = (Item) getItem(position);
        ItemView cell = (ItemView) convertView;

        if (cell == null) {
            cell = item.newView(mContext, null);
            cell.prepareItemView();
        }

        cell.setObject(item);

        return (View) cell;
    }

}
