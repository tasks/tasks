/*
 * Copyright (C) 2011 Cyril Mottier (http://www.cyrilmottier.com)
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
import greendroid.widget.AsyncImageView;
import android.os.Bundle;
import android.view.View;

public class SimpleAsyncImageViewActivity extends GDActivity {
    
    private static final String URLS_1 = "https://lh3.googleusercontent.com/_OHO4y8YcQbs/SoWDYIhFrjI/AAAAAAAAKX4/ETS4JGuUYX0/s400/P1080412.JPG";
    private static final String URLS_2 = "https://lh6.googleusercontent.com/_OHO4y8YcQbs/So4a6aWih3I/AAAAAAAAKts/hGFcqaHsCuI/s400/P1080809.JPG";
    private static final String URLS_3 = "https://lh4.googleusercontent.com/_OHO4y8YcQbs/SSYGHclokDI/AAAAAAAAFUs/qNyvU-4o5eI/s400/P1040275.JPG";
    
    private AsyncImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarContentView(R.layout.image);
        mImageView = (AsyncImageView) findViewById(R.id.image_view);
    }
    
    public void onShowImage1(View v) {
        mImageView.setUrl(URLS_1);
    }
    
    public void onShowImage2(View v) {
        mImageView.setUrl(URLS_2);
    }
    
    public void onShowImage3(View v) {
        mImageView.setUrl(URLS_3);
    }
}
