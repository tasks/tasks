package com.todoroo.andlib.utility;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;

/**
 * Android Utility Classes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AndroidUtilities {

    // --- activity management

    private static Activity currentlyActive = null;

    public static void setCurrentlyActive(Activity currentlyActive) {
        AndroidUtilities.currentlyActive = currentlyActive;
    }

    /**
     * @return our best-guess currently active activity. Maybe null, may
     * be out of view already.
     */
    public static Activity getCurrentlyActiveActivity() {
        return currentlyActive;
    }

    // --- utility methods

    private static class ExceptionHelper {
        @Autowired
        public ExceptionService exceptionService;

        public ExceptionHelper() {
            DependencyInjectionService.getInstance().inject(this);
        }
    }

    /**
     * @return true if we're connected to the internet
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager)
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null)
            return false;
        if (info.getState() != State.CONNECTED)
            return false;
        return true;
    }

    /** Fetch the image specified by the given url */
    public static Bitmap fetchImage(URL url) throws IOException {
        InputStream is = null;
        try {
            URLConnection conn = url.openConnection();
            conn.connect();
            is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is, 16384);
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(bis);
                return bitmap;
            } finally {
                bis.close();
            }
        } finally {
            if(is != null)
                is.close();
        }
    }

    /**
     * Start the given intent, handling security exceptions if they arise
     *
     * @param context
     * @param intent
     * @param request request code. if negative, no request.
     */
    public static void startExternalIntent(Context context, Intent intent, int request) {
        try {
            if(request > -1 && context instanceof Activity)
                ((Activity)context).startActivityForResult(intent, request);
            else
                context.startActivity(intent);
        } catch (SecurityException e) {
            ExceptionHelper helper = new ExceptionHelper();
            helper.exceptionService.displayAndReportError(context,
                    "start-external-intent-" + intent.toString(), //$NON-NLS-1$
                    e);
        }
    }

    /**
     * Start the given intent, handling security exceptions if they arise
     *
     * @param activity
     * @param intent
     * @param requestCode
     */
    public static void startExternalIntentForResult(
            Activity activity, Intent intent, int requestCode) {
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (SecurityException e) {
            ExceptionHelper helper = new ExceptionHelper();
            helper.exceptionService.displayAndReportError(activity,
                    "start-external-intent-" + intent.toString(), //$NON-NLS-1$
                    e);
        }
    }

    /**
     * Put an arbitrary object into a {@link ContentValues}
     * @param target
     * @param key
     * @param value
     */
    public static void putInto(ContentValues target, String key, Object value) {
        if(value instanceof String)
            target.put(key, (String) value);
        else if(value instanceof Long)
            target.put(key, (Long) value);
        else if(value instanceof Integer)
            target.put(key, (Integer) value);
        else if(value instanceof Double)
            target.put(key, (Double) value);
        else
            throw new UnsupportedOperationException("Could not handle type " + //$NON-NLS-1$
                    value.getClass());
    }

    /**
     * Rips apart a content value into two string arrays, keys and value
     */
    public static String[][] contentValuesToStringArrays(ContentValues source) {
        String[][] result = new String[2][source.size()];
        int i = 0;
        for(Entry<String, Object> entry : source.valueSet()) {
            result[0][i] = entry.getKey();
            result[1][i++] = entry.getValue().toString();
        }
        return result;
    }

    /**
     * Return index of value in array
     * @param array array to search
     * @param value value to look for
     * @return
     */
    public static <TYPE> int indexOf(TYPE[] array, TYPE value) {
        for(int i = 0; i < array.length; i++)
            if(array[i].equals(value))
                return i;
        return -1;
    }

    /**
     * Turn ContentValues into a string
     * @param string
     * @return
     */
    @SuppressWarnings("nls")
    public static ContentValues contentValuesFromString(String string) {
        if(string == null)
            return null;

        String[] pairs = string.split(",");
        ContentValues result = new ContentValues();
        for(String item : pairs) {
            String[] keyValue = item.split("=");
            result.put(keyValue[0].trim(), keyValue[1].trim());
        }
        return result;
    }

    /**
     * Returns true if a and b or null or a.equals(b)
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(Object a, Object b) {
        if(a == null && b == null)
            return true;
        if(a == null)
            return false;
        return a.equals(b);
    }

    /**
     * Copy a file from one place to another
     *
     * @param in
     * @param out
     * @throws Exception
     */
    public static void copyFile(File in, File out) throws Exception {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            fis.close();
            fos.close();
        }
    }
}
