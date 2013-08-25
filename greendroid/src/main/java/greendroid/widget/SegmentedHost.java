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

import greendroid.util.Config;
import greendroid.widget.SegmentedBar.OnSegmentChangeListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.cyrilmottier.android.greendroid.R;

/**
 * A SegmentedHost is a wrapper view that handle a SegmentedBar and a
 * FrameLayout that hold the content. Data (titles, and content Views) are
 * provided to the SegmentedHost via a {@link SegmentedAdapter}.
 * 
 * @author Cyril Mottier
 */
public class SegmentedHost extends LinearLayout {

    private final static String LOG_TAG = SegmentedHost.class.getSimpleName();

    private int mSegmentedBarId;
    private SegmentedBar mSegmentedBar;
    private int mSegmentedHostId;
    private FrameLayout mContentView;
    private int mSelectedSegment;

    private SegmentedAdapter mAdapter;
    private View[] mViews;
    private DataSetObserver mSegmentObserver = new DataSetObserver() {

        public void onChanged() {
            setupSegmentedHost(mSelectedSegment);
        }

        public void onInvalidated() {
            // Do nothing - method never called
        }
    };

    public SegmentedHost(Context context) {
        this(context, null);
    }

    public SegmentedHost(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.gdSegmentedHostStyle);
    }

    public SegmentedHost(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        initSegmentedView();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SegmentedHost, defStyle, 0);

        mSegmentedBarId = a.getResourceId(R.styleable.SegmentedHost_segmentedBar, -1);
        if (mSegmentedBarId <= 0) {
            throw new IllegalArgumentException("The segmentedBar attribute is required and must refer "
                    + "to a valid child.");
        }

        mSegmentedHostId = a.getResourceId(R.styleable.SegmentedHost_segmentedContentView, -1);
        if (mSegmentedHostId <= 0) {
            throw new IllegalArgumentException("The segmentedHost attribute is required and must refer "
                    + "to a valid child.");
        }
    }

    private void initSegmentedView() {
        setOrientation(LinearLayout.VERTICAL);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSegmentedBar = (SegmentedBar) findViewById(mSegmentedBarId);
        if (mSegmentedBar == null) {
            throw new IllegalArgumentException("The segmentedBar attribute must refer to an existing child.");
        }
        mSegmentedBar.setOnSegmentChangeListener(new SegmentSwitcher());

        mContentView = (FrameLayout) findViewById(mSegmentedHostId);
        if (mContentView == null) {
            throw new IllegalArgumentException("The segmentedHost attribute must refer to an existing child.");
        } else if (!(mContentView instanceof FrameLayout)) {
            throw new RuntimeException("The segmentedHost attribute must refer to a FrameLayout");

        }
    }

    public SegmentedBar getSegmentedBar() {
        return mSegmentedBar;
    }

    public FrameLayout getContentView() {
        return mContentView;
    }

    public void setAdapter(SegmentedAdapter adapter) {

        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mSegmentObserver);
        }
        mAdapter = adapter;

        if (adapter != null) {
            mAdapter.registerDataSetObserver(mSegmentObserver);
        }
        setupSegmentedHost(0);
    }

    private void setupSegmentedHost(int selectedSegment) {

        if (Config.GD_INFO_LOGS_ENABLED) {
            Log.i(LOG_TAG, "Preparing the SegmentedHost with the segment " + selectedSegment);
        }

        mSegmentedBar.removeAllViews();
        mContentView.removeAllViews();
        mViews = null;

        if (mAdapter != null) {
            // Add all segments to the Segmentedbar
            final int count = mAdapter.getCount();
            for (int i = 0; i < count; i++) {
                mSegmentedBar.addSegment(mAdapter.getSegmentTitle(i));
            }

            if (selectedSegment < 0) {
                selectedSegment = 0;
            } else if (selectedSegment > count) {
                selectedSegment = count;
            }

            if (count > 0) {
                // Prepare the SegmentBar
                mViews = new View[count];
                mSegmentedBar.setCurrentSegment(selectedSegment);

                // And displays the first view
                setContentView(selectedSegment);
            }
        }
    }

    private class SegmentSwitcher implements OnSegmentChangeListener {
        public void onSegmentChange(int index, boolean clicked) {
            setContentView(index);
        }
    }

    private void setContentView(int index) {
        mSelectedSegment = index;
        mContentView.removeAllViews();
        if (mViews[index] == null) {
            mViews[index] = mAdapter.getView(index, SegmentedHost.this);
        }
        mContentView.addView(mViews[index]);
    }

}
