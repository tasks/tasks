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

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

/**
 * A SegmentedAdapter is a data source of a SegmentedHost/SegmentedHost.
 * 
 * @author Cyril Mottier
 */
public abstract class SegmentedAdapter {

    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    /**
     * How many segments have to be displayed
     * 
     * @return The number of segment displayed by the underlying SegmentedBar
     */
    public abstract int getCount();

    /**
     * Get the View associated to the segment at position <em>position</em>
     * 
     * @param position The position of the item in the SegmentedAdapter
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the segment at the given position
     */
    public abstract View getView(int position, ViewGroup parent);

    /**
     * Get the title for the segment at position <em>position</em>
     * 
     * @param position The position of the segment in the SegmentedBar
     * @return A title for the segment at the given position.
     */
    public abstract String getSegmentTitle(int position);

    /**
     * Register an observer that is called when changes happen to the data used
     * by this adapter.
     * 
     * @param observer The object that gets notified when the data set changes.
     */
    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    /**
     * Unregister an observer that has previously been registered with this
     * adapter via {@link #unregisterDataSetObserver(DataSetObserver)}
     * 
     * @param observer The object to unregister
     */
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    /**
     * Notifies the attached View that the underlying data has changed and
     * should refresh itself.
     */
    public void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }
}
