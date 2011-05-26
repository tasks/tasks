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

import greendroid.app.GDTabActivity;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

public class TabbedActionBarActivity extends GDTabActivity {

    private static final String TAB1 = "tab_one";
    private static final String TAB2 = "tab_two";
    private static final String TAB3 = "tab_three";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle("Screen 2");

        addTab(TAB1, "Tab One", Color.BLACK, "Content of tab #1");
        addTab(TAB2, "Tab Two", Color.rgb(20, 20, 20), "Content of tab #2");
        addTab(TAB3, "Tab Three", Color.rgb(40, 40, 40), "Content of tab #3");
    }
    
    private void addTab(String tag, CharSequence label, int color, String text) {
        final Intent intent = new Intent(this, FakeActivity.class);
        intent.putExtra(FakeActivity.EXTRA_COLOR, color);
        intent.putExtra(FakeActivity.EXTRA_TEXT, text);
        addTab(tag, label, intent);
    }

    public static class FakeActivity extends Activity {

        public static final String EXTRA_COLOR = "com.cyrilmottier.android.gdcatalog.TabbedActionBarActivity$FakeActivity.extraColor";
        public static final String EXTRA_TEXT = "com.cyrilmottier.android.gdcatalog.TabbedActionBarActivity$FakeActivity.extraText";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final Intent intent = getIntent();

            if (intent != null) {
                setContentView(R.layout.text);

                TextView textView = (TextView) findViewById(R.id.text);
                textView.setText(intent.getStringExtra(EXTRA_TEXT));
                textView.setBackgroundColor(intent.getIntExtra(EXTRA_COLOR, Color.WHITE));
            }
        }

    }

}
