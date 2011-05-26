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
import greendroid.widget.ActionBarItem;
import greendroid.widget.LoaderActionBarItem;
import greendroid.widget.ActionBarItem.Type;
import greendroid.widget.NormalActionBarItem;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

public class ActionBarActivity extends GDActivity {

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setActionBarContentView(R.layout.text);
        ((TextView) findViewById(R.id.text)).setText(R.string.first_screen);

        addActionBarItem(Type.Refresh, R.id.action_bar_refresh);
        addActionBarItem(getActionBar()
                .newActionBarItem(NormalActionBarItem.class)
                .setDrawable(R.drawable.ic_title_export)
                .setContentDescription(R.string.gd_export), R.id.action_bar_export);
        addActionBarItem(Type.Locate, R.id.action_bar_locate);
    }

    @Override
    public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {

        switch (item.getItemId()) {
            case R.id.action_bar_locate:
                startActivity(new Intent(this, TabbedActionBarActivity.class));
                break;

            case R.id.action_bar_refresh:
                final LoaderActionBarItem loaderItem = (LoaderActionBarItem) item;
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        loaderItem.setLoading(false);
                    }
                }, 2000);
                Toast.makeText(this, R.string.refresh_pressed, Toast.LENGTH_SHORT).show();
                break;

            case R.id.action_bar_export:
                Toast.makeText(this, R.string.custom_drawable, Toast.LENGTH_SHORT).show();
                break;

            default:
                return super.onHandleActionBarItemClick(item, position);
        }

        return true;
    }
}
