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
package com.cyrilmottier.android.gdcatalog;

import greendroid.app.GDActivity;
import greendroid.widget.SegmentedAdapter;
import greendroid.widget.SegmentedHost;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SegmentedActivity extends GDActivity {

    private final Handler mHandler = new Handler();
    private PeopleSegmentedAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setActionBarContentView(R.layout.segmented_controls);

        SegmentedHost segmentedHost = (SegmentedHost) findViewById(R.id.segmented_host);

        mAdapter = new PeopleSegmentedAdapter();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mAdapter.mReverse = true;
                mAdapter.notifyDataSetChanged();
            }
        }, 4000);

        segmentedHost.setAdapter(mAdapter);
    }

    private class PeopleSegmentedAdapter extends SegmentedAdapter {

        public boolean mReverse = false;

        @Override
        public View getView(int position, ViewGroup parent) {
            TextView textView = (TextView) getLayoutInflater().inflate(R.layout.text, parent, false);
            textView.setText(getSegmentTitle(position));
            return textView;
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public String getSegmentTitle(int position) {

            switch (mReverse ? ((getCount() - 1) - position) : position) {
                case 0:
                    return getString(R.string.segment_1);
                case 1:
                    return getString(R.string.segment_2);
                case 2:
                    return getString(R.string.segment_3);
                case 3:
                    return getString(R.string.segment_4);
            }

            return null;
        }
    }

}
