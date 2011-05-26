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

import greendroid.app.GDListActivity;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.DescriptionItem;
import greendroid.widget.item.DrawableItem;
import greendroid.widget.item.Item;
import greendroid.widget.item.ProgressItem;
import greendroid.widget.item.SeparatorItem;
import greendroid.widget.item.TextItem;
import greendroid.widget.item.ThumbnailItem;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;

public class BasicItemActivity extends GDListActivity {
    
    private final Handler mHandler = new Handler();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        List<Item> items = new ArrayList<Item>();
        
        items.add(new SeparatorItem("Class 1"));
        items.add(new ThumbnailItem("Powered paragliding", "aka paramotoring", R.drawable.class1));
        items.add(new DescriptionItem("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed tempus consequat leo, et tincidunt justo tristique in."));
        
        items.add(new SeparatorItem("Class 2"));
        items.add(new DrawableItem("Trikes", R.drawable.class2));
        items.add(new DescriptionItem("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed tempus consequat leo, et tincidunt justo tristique in."));
        
        items.add(new SeparatorItem("Class 3"));
        items.add(new ThumbnailItem("Multi-axis", "Looks like a tiny plane", R.drawable.class3));
        items.add(new DescriptionItem("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed tempus consequat leo, et tincidunt justo tristique in."));
        
        items.add(new SeparatorItem("Class 4"));
        items.add(new ThumbnailItem("Auto-gyro", "A scary helicopter", R.drawable.class4));
        items.add(new DescriptionItem("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed tempus consequat leo, et tincidunt justo tristique in."));
        
        items.add(new SeparatorItem("Class 5"));
        items.add(new DrawableItem("Hot air baloon", R.drawable.class5));
        items.add(new DescriptionItem("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed tempus consequat leo, et tincidunt justo tristique in."));
        
        final Item item1 = new SeparatorItem("Class 6");
        final Item item2 = new TextItem("Airbus/Boeing planes");
        final Item item3 = new DescriptionItem("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed tempus consequat leo, et tincidunt justo tristique in.");
        items.add(item1);
        items.add(item2);
        items.add(item3);
        
        final ProgressItem progressItem = new ProgressItem("Removing intruders", true);
        items.add(progressItem);
        
        final ItemAdapter adapter = new ItemAdapter(this, items);
        setListAdapter(adapter);
        
        mHandler.postDelayed(new Runnable() {
            public void run() {
                adapter.remove(item1);
                adapter.remove(item2);
                adapter.remove(item3);
                adapter.remove(progressItem);
                adapter.insert(new ThumbnailItem("Ultralight aviation", "List of French 'ULM' classes", R.drawable.ic_gdcatalog), 0);
                adapter.notifyDataSetChanged();
            }
        },8000);
    }
}
