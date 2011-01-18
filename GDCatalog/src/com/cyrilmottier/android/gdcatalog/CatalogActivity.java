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
import greendroid.widget.ItemAdapter;
import greendroid.widget.ActionBar.Type;
import greendroid.widget.item.TextItem;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class CatalogActivity extends GDActivity {

    private ListView mListView;
    private Class<?>[] mDemoClasses = {
            BasicItemActivity.class, XmlItemActivity.class, TweakedItemViewActivity.class, SegmentedActivity.class,
            ActionBarActivity.class, QuickActionActivity.class
    };

    public CatalogActivity() {
        super(Type.Dashboard);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setActionBarContentView(R.layout.list);

        ItemAdapter adapter = new ItemAdapter(this);
        adapter.add(new TextItem("Basic items"));
        adapter.add(new TextItem("XML items"));
        adapter.add(new TextItem("Tweaked item cell"));
        adapter.add(new TextItem("SegmentedBar"));
        adapter.add(new TextItem("ActionBarActivity"));
        adapter.add(new TextItem("QuickActionActivity"));
        
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(mItemClickHandler);
    }

    private OnItemClickListener mItemClickHandler = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position >= 0 && position < mDemoClasses.length) {
                Intent intent = new Intent(CatalogActivity.this, mDemoClasses[position]);
                
                switch (position) {
                    case 4:
                        intent.putExtra(greendroid.app.ActionBarActivity.GD_ACTION_BAR_TITLE, "ActionBarActivity");
                        break;
                }
                
                startActivity(intent);
            }
        }
    };

}
