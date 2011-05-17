package com.todoroo.andlib.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Stack;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.todoroo.astrid.api.R;

/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

public class ImageLoader {

    // the simplest in-memory cache implementation. This should be replaced with
    // something like SoftReference or BitmapOptions.inPurgeable(since 1.6)
    private final HashMap<String, Uri> cache = new HashMap<String, Uri>();

    private File cacheDir;

    public ImageLoader(Context context) {
        // Make the background thread low priority. This way it will not affect
        // the UI performance
        photoLoaderThread.setPriority(Thread.NORM_PRIORITY - 1);

        // Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED))
            cacheDir = new File(
                    android.os.Environment.getExternalStorageDirectory(),
                    "Android/data/com.todoroo.astrid/cache/"); //$NON-NLS-1$
        else
            cacheDir = context.getCacheDir();
        if (!cacheDir.exists())
            cacheDir.mkdirs();
    }

    final int stub_id = R.drawable.image_placeholder;

    public void displayImage(String url, ImageView imageView) {
        if (cache.containsKey(url))
            imageView.setImageURI(cache.get(url));
        else {
            queuePhoto(url, imageView);
            imageView.setImageResource(stub_id);
        }
    }

    private void queuePhoto(String url, ImageView imageView) {
        // This ImageView may be used for other images before. So there may be
        // some old tasks in the queue. We need to discard them.
        photosQueue.Clean(imageView);
        PhotoToLoad p = new PhotoToLoad(url, imageView);
        synchronized (photosQueue.photosToLoad) {
            photosQueue.photosToLoad.push(p);
            photosQueue.photosToLoad.notifyAll();
        }

        // start thread if it's not started yet
        if (photoLoaderThread.getState() == Thread.State.NEW)
            photoLoaderThread.start();
    }

    private Uri getUri(String url) {
        if(!TextUtils.isEmpty(url) && url.contains("://")) { //$NON-NLS-1$
            // identify images by hashcode. Not a perfect solution.
            String filename = String.valueOf(url.hashCode());
            File f = new File(cacheDir, filename);

            // from SD cache
            if (f.exists()) {
                Uri b = Uri.fromFile(f);
                System.out.println(f.toString());
                return b;
            }

            // from web
            try {
                Uri bitmap = null;
                InputStream is = new URL(url).openStream();
                OutputStream os = new FileOutputStream(f);
                AndroidUtilities.copyStream(is, os);
                os.close();
                bitmap = Uri.fromFile(f);
                System.out.println(f.toString());
                return bitmap;
            } catch (Exception e) {
                Log.e("imagel-loader", "Unable to get URL", e); //$NON-NLS-1$ //$NON-NLS-2$
                return null;
            }
        } else {
            return null;
        }
    }

    // Task for the queue
    private class PhotoToLoad {
        public String url;
        public ImageView imageView;

        public PhotoToLoad(String u, ImageView i) {
            url = u;
            imageView = i;
            imageView.setTag(url);
        }
    }

    PhotosQueue photosQueue = new PhotosQueue();

    public void stopThread() {
        photoLoaderThread.interrupt();
    }

    // stores list of photos to download
    class PhotosQueue {
        private final Stack<PhotoToLoad> photosToLoad = new Stack<PhotoToLoad>();

        // removes all instances of this ImageView
        public void Clean(ImageView image) {
            for (int j = 0; j < photosToLoad.size();) {
                if (photosToLoad.get(j).imageView == image)
                    photosToLoad.remove(j);
                else
                    ++j;
            }
        }
    }

    class PhotosLoader extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    // thread waits until there are any images to load in the
                    // queue
                    if (photosQueue.photosToLoad.size() == 0)
                        synchronized (photosQueue.photosToLoad) {
                            photosQueue.photosToLoad.wait();
                        }
                    if (photosQueue.photosToLoad.size() != 0) {
                        PhotoToLoad photoToLoad;
                        synchronized (photosQueue.photosToLoad) {
                            photoToLoad = photosQueue.photosToLoad.pop();
                        }
                        Uri bmp = getUri(photoToLoad.url);
                        cache.put(photoToLoad.url, bmp);
                        if (((String) photoToLoad.imageView.getTag()).equals(photoToLoad.url)) {
                            UriDisplayer bd = new UriDisplayer(bmp,
                                    photoToLoad.imageView);
                            Activity a = (Activity) photoToLoad.imageView.getContext();
                            a.runOnUiThread(bd);
                        }
                    }
                    if (Thread.interrupted())
                        break;
                }
            } catch (InterruptedException e) {
                // allow thread to exit
            }
        }
    }

    PhotosLoader photoLoaderThread = new PhotosLoader();

    class UriDisplayer implements Runnable {
        Uri uri;
        ImageView imageView;

        public UriDisplayer(Uri u, ImageView i) {
            uri = u;
            imageView = i;
        }

        public void run() {
            if(uri == null)
                return;

            File f = new File(uri.getPath());
            if (f.exists()) {
                imageView.setImageURI(Uri.parse(f.toString()));
            } else {
                imageView.setImageResource(stub_id);
            }

        }
    }

    public void clearCache() {
        // clear memory cache
        cache.clear();

        // clear SD cache
        File[] files = cacheDir.listFiles();
        for (File f : files)
            f.delete();
    }

}