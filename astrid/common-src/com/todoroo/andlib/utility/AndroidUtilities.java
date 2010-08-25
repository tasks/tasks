package com.todoroo.andlib.utility;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Comparator;
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
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;

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

    public static final String SEPARATOR_ESCAPE = "!PIPE!"; //$NON-NLS-1$
    public static final String SERIALIZATION_SEPARATOR = "|"; //$NON-NLS-1$

    // --- utility methods

    private static class ExceptionHelper {
        @Autowired
        public ExceptionService exceptionService;

        public ExceptionHelper() {
            DependencyInjectionService.getInstance().inject(this);
        }
    }

    /** Suppress virtual keyboard until user's first tap */
    public static void suppressVirtualKeyboard(final TextView editor) {
        final int inputType = editor.getInputType();
        editor.setInputType(InputType.TYPE_NULL);
        editor.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                editor.setInputType(inputType);
                editor.setOnTouchListener(null);
                return false;
            }
        });
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
        } catch (Exception e) {
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
     * Serializes a content value into a string
     */
    public static String contentValuesToSerializedString(ContentValues source) {
        StringBuilder result = new StringBuilder();
        for(Entry<String, Object> entry : source.valueSet()) {
            result.append(entry.getKey().replace(SERIALIZATION_SEPARATOR, SEPARATOR_ESCAPE)).append(
                    SERIALIZATION_SEPARATOR);
            Object value = entry.getValue();
            if(value instanceof Integer)
                result.append('i').append(value);
            else if(value instanceof Double)
                result.append('d').append(value);
            else if(value instanceof Long)
                result.append('l').append(value);
            else if(value instanceof String)
                result.append('s').append(value.toString());
            else
                throw new UnsupportedOperationException(value.getClass().toString());
            result.append(SERIALIZATION_SEPARATOR);
        }
        return result.toString();
    }

    /**
     * Turn ContentValues into a string
     * @param string
     * @return
     */
    public static ContentValues contentValuesFromSerializedString(String string) {
        if(string == null)
            return new ContentValues();

        String[] pairs = string.split("\\" + SERIALIZATION_SEPARATOR); //$NON-NLS-1$
        ContentValues result = new ContentValues();
        for(int i = 0; i < pairs.length; i += 2) {
            String key = pairs[i].replaceAll(SEPARATOR_ESCAPE, SERIALIZATION_SEPARATOR);
            String value = pairs[i+1].substring(1);
            switch(pairs[i+1].charAt(0)) {
            case 'i':
                result.put(key, Integer.parseInt(value));
                break;
            case 'd':
                result.put(key, Double.parseDouble(value));
                break;
            case 'l':
                result.put(key, Long.parseLong(value));
                break;
            case 's':
                result.put(key, value.replace(SEPARATOR_ESCAPE, SERIALIZATION_SEPARATOR));
                break;
            }
        }
        return result;
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

        String[] pairs = string.split("=");
        ContentValues result = new ContentValues();
        String key = null;
        for(int i = 0; i < pairs.length; i++) {
            String newKey = null;
            int lastSpace = pairs[i].lastIndexOf(' ');
            if(lastSpace != -1) {
                newKey = pairs[i].substring(lastSpace + 1);
                pairs[i] = pairs[i].substring(0, lastSpace);
            } else {
                newKey =  pairs[i];
            }
            if(key != null)
                result.put(key.trim(), pairs[i].trim());
            key = newKey;
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

    /**
     * Find a child view of a certain type
     * @param view
     * @param type
     * @return first view (by DFS) if found, or null if none
     */
    public static <TYPE> TYPE findViewByType(View view, Class<TYPE> type) {
        if(view == null)
            return null;
        if(type.isInstance(view))
            return (TYPE) view;
        if(view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for(int i = 0; i < group.getChildCount(); i++) {
                TYPE v = findViewByType(group.getChildAt(i), type);
                if(v != null)
                    return v;
            }
        }
        return null;
    }

    /**
     * @return Android SDK version as an integer. Works on all versions
     */
    public static int getSdkVersion() {
        return Integer.parseInt(android.os.Build.VERSION.SDK);
    }

    /**
     * Copy databases to a given folder. Useful for debugging
     * @param folder
     */
    public static void copyDatabases(Context context, String folder) {
        File folderFile = new File(folder);
        if(!folderFile.exists())
            folderFile.mkdir();
        for(String db : context.databaseList()) {
            File dbFile = context.getDatabasePath(db);
            try {
                copyFile(dbFile, new File(folderFile.getAbsolutePath() +
                        File.separator + db));
            } catch (Exception e) {
                Log.e("ERROR", "ERROR COPYING DB " + db, e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    /**
     * Sort files by date so the newest file is on top
     * @param files
     */
    public static void sortFilesByDateDesc(File[] files) {
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return Long.valueOf(o2.lastModified()).compareTo(Long.valueOf(o1.lastModified()));
            }
        });
    }

    /**
     * Sleep, ignoring interruption
     * @param l
     */
    public static void sleepDeep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
