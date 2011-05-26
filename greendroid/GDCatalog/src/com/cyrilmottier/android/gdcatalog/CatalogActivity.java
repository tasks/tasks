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
import greendroid.graphics.drawable.ActionBarDrawable;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ItemAdapter;
import greendroid.widget.NormalActionBarItem;
import greendroid.widget.item.TextItem;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class CatalogActivity extends GDListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ItemAdapter adapter = new ItemAdapter(this);
        adapter.add(createTextItem(R.string.basic_item_label, BasicItemActivity.class));
        adapter.add(createTextItem(R.string.xml_item_label, XmlItemActivity.class));
        adapter.add(createTextItem(R.string.tweaked_item_view_label, TweakedItemViewActivity.class));
        adapter.add(createTextItem(R.string.segmented_label, SegmentedActivity.class));
        adapter.add(createTextItem(R.string.action_bar_activity_label, ActionBarActivity.class));
        adapter.add(createTextItem(R.string.quick_action_label, QuickActionActivity.class));
        adapter.add(createTextItem(R.string.simple_async_image_view_label, SimpleAsyncImageViewActivity.class));
        adapter.add(createTextItem(R.string.async_image_view_list_view_label, AsyncImageViewListActivity.class));

        setListAdapter(adapter);

        addActionBarItem(getActionBar()
                .newActionBarItem(NormalActionBarItem.class)
                .setDrawable(new ActionBarDrawable(getResources(), R.drawable.ic_action_bar_info)), R.id.action_bar_view_info);
    }

    private TextItem createTextItem(int stringId, Class<?> klass) {
        final TextItem textItem = new TextItem(getString(stringId));
        textItem.setTag(klass);
        return textItem;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final TextItem textItem = (TextItem) l.getAdapter().getItem(position);
        Intent intent = new Intent(CatalogActivity.this, (Class<?>) textItem.getTag());
        intent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, textItem.text);
        startActivity(intent);
    }

    @Override
    public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
        switch (item.getItemId()) {
            case R.id.action_bar_view_info:
                startActivity(new Intent(this, InfoTabActivity.class));
                return true;

            default:
                return super.onHandleActionBarItemClick(item, position);
        }
    }
}
