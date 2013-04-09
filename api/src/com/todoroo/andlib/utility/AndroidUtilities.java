/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupWindow;
import android.widget.TextView;

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

    /** Read a bitmap from the specified file, scaling if necessary
     *  Returns null if scaling failed after several tries */
    private static final int[] SAMPLE_SIZES = { 1, 2, 4, 6, 8, 10 };
    private static final int MAX_DIM = 1024;
    public static Bitmap readScaledBitmap(String file) {
        Bitmap bitmap = null;
        int tries = 0;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        while((bitmap == null || (bitmap.getWidth() > MAX_DIM || bitmap.getHeight() > MAX_DIM)) && tries < SAMPLE_SIZES.length) {
            opts.inSampleSize = SAMPLE_SIZES[tries];
            try {
                bitmap = BitmapFactory.decodeFile(file, opts);
            } catch (OutOfMemoryError e) {
                // Too big
                Log.e("decode-bitmap", "Out of memory with sample size " + opts.inSampleSize, e);  //$NON-NLS-1$//$NON-NLS-2$
            }
            tries++;
        }

        return bitmap;
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
            getExceptionService().displayAndReportError(context,
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
            getExceptionService().displayAndReportError(activity,
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
    public static void putInto(ContentValues target, String key, Object value, boolean errorOnFail) {
        if (value instanceof Boolean)
            target.put(key, (Boolean) value);
        else if (value instanceof Byte)
            target.put(key, (Byte) value);
        else if (value instanceof Double)
            target.put(key, (Double) value);
        else if (value instanceof Float)
            target.put(key, (Float) value);
        else if (value instanceof Integer)
            target.put(key, (Integer) value);
        else if (value instanceof Long)
            target.put(key, (Long) value);
        else if (value instanceof Short)
            target.put(key, (Short) value);
        else if (value instanceof String)
            target.put(key, (String) value);
        else if (errorOnFail)
            throw new UnsupportedOperationException("Could not handle type " + //$NON-NLS-1$
                    value.getClass());
    }

    /**
     * Put an arbitrary object into a {@link ContentValues}
     * @param target
     * @param key
     * @param value
     */
    public static void putInto(Bundle target, String key, Object value, boolean errorOnFail) {
        if (value instanceof Boolean)
            target.putBoolean(key, (Boolean) value);
        else if (value instanceof Byte)
            target.putByte(key, (Byte) value);
        else if (value instanceof Double)
            target.putDouble(key, (Double) value);
        else if (value instanceof Float)
            target.putFloat(key, (Float) value);
        else if (value instanceof Integer)
            target.putInt(key, (Integer) value);
        else if (value instanceof Long)
            target.putLong(key, (Long) value);
        else if (value instanceof Short)
            target.putShort(key, (Short) value);
        else if (value instanceof String)
            target.putString(key, (String) value);
        else if (errorOnFail)
            throw new UnsupportedOperationException("Could not handle type " + //$NON-NLS-1$
                    value.getClass());
    }

    // --- serialization

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
     * Return index of value in integer array
     * @param array array to search
     * @param value value to look for
     * @return
     */
    public static int indexOf(int[] array, int value) {
        for (int i = 0; i < array.length; i++)
            if (array[i] == value)
                return i;
        return -1;
    }

    /**
     * Serializes a content value into a string
     */
    public static String contentValuesToSerializedString(ContentValues source) {
        StringBuilder result = new StringBuilder();
        for(Entry<String, Object> entry : source.valueSet()) {
            addSerialized(result, entry.getKey(), entry.getValue());
        }
        return result.toString();
    }

    /** add serialized helper */
    private static void addSerialized(StringBuilder result,
            String key, Object value) {
        result.append(key.replace(SERIALIZATION_SEPARATOR, SEPARATOR_ESCAPE)).append(
                SERIALIZATION_SEPARATOR);
        if(value instanceof Integer)
            result.append('i').append(value);
        else if(value instanceof Double)
            result.append('d').append(value);
        else if(value instanceof Long)
            result.append('l').append(value);
        else if(value instanceof String)
            result.append('s').append(value.toString().replace(SERIALIZATION_SEPARATOR, SEPARATOR_ESCAPE));
        else if (value instanceof Boolean)
            result.append('b').append(value);
        else
            throw new UnsupportedOperationException(value.getClass().toString());
        result.append(SERIALIZATION_SEPARATOR);
    }

    /**
     * Serializes a {@link android.os.Bundle} into a string
     */
    public static String bundleToSerializedString(Bundle source) {
        StringBuilder result = new StringBuilder();
        if (source == null)
            return null;

        for(String key : source.keySet()) {
            addSerialized(result, key, source.get(key));
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

        ContentValues result = new ContentValues();
        fromSerialized(string, result, new SerializedPut<ContentValues>() {
            public void put(ContentValues object, String key, char type, String value) throws NumberFormatException {
                switch(type) {
                case 'i':
                    object.put(key, Integer.parseInt(value));
                    break;
                case 'd':
                    object.put(key, Double.parseDouble(value));
                    break;
                case 'l':
                    object.put(key, Long.parseLong(value));
                    break;
                case 's':
                    object.put(key, value.replace(SEPARATOR_ESCAPE, SERIALIZATION_SEPARATOR));
                    break;
                case 'b':
                    object.put(key, Boolean.parseBoolean(value));
                    break;
                }
            }
        });
        return result;
    }

    /**
     * Turn {@link android.os.Bundle} into a string
     * @param string
     * @return
     */
    public static Bundle bundleFromSerializedString(String string) {
        if(string == null)
            return new Bundle();

        Bundle result = new Bundle();
        fromSerialized(string, result, new SerializedPut<Bundle>() {
            public void put(Bundle object, String key, char type, String value) throws NumberFormatException {
                switch(type) {
                case 'i':
                    object.putInt(key, Integer.parseInt(value));
                    break;
                case 'd':
                    object.putDouble(key, Double.parseDouble(value));
                    break;
                case 'l':
                    object.putLong(key, Long.parseLong(value));
                    break;
                case 's':
                    object.putString(key, value.replace(SEPARATOR_ESCAPE, SERIALIZATION_SEPARATOR));
                    break;
                case 'b':
                    object.putBoolean(key, Boolean.parseBoolean(value));
                    break;
                }
            }
        });
        return result;
    }

    public interface SerializedPut<T> {
        public void put(T object, String key, char type, String value) throws NumberFormatException;
    }

    @SuppressWarnings("nls")
    private static <T> void fromSerialized(String string, T object, SerializedPut<T> putter) {
        String[] pairs = string.split("\\" + SERIALIZATION_SEPARATOR); //$NON-NLS-1$
        for(int i = 0; i < pairs.length; i += 2) {
            try {
                String key = pairs[i].replaceAll(SEPARATOR_ESCAPE, SERIALIZATION_SEPARATOR);
                String value = pairs[i+1].substring(1);
                try {
                    putter.put(object, key, pairs[i+1].charAt(0), value);
                } catch (NumberFormatException e) {
                    // failed parse to number
                    putter.put(object, key, 's', value);
                }
            } catch (IndexOutOfBoundsException e) {
                Log.e("deserialize", "Badly formed serialization: " + string, e);
            }
        }
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
            copyStream(fis, fos);
        } catch (Exception e) {
            throw e;
        } finally {
            fis.close();
            fos.close();
        }
    }

    /**
     * Copy stream from source to destination
     * @param source
     * @param dest
     * @throws IOException
     */
    public static void copyStream(InputStream source, OutputStream dest) throws IOException {
        int bytes;
        byte[] buffer;
        int BUFFER_SIZE = 1024;
        buffer = new byte[BUFFER_SIZE];
        while ((bytes = source.read(buffer)) != -1) {
            if (bytes == 0) {
                bytes = source.read();
                if (bytes < 0)
                    break;
                dest.write(bytes);
                dest.flush();
                continue;
            }

            dest.write(buffer, 0, bytes);
            dest.flush();
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
     * Search for the given value in the map, returning key if found
     * @param map
     * @param value
     * @return null if not found, otherwise key
     */
    public static <KEY, VALUE> KEY findKeyInMap(Map<KEY, VALUE> map, VALUE value){
        for (Entry<KEY, VALUE> entry: map.entrySet()) {
            if(entry.getValue().equals(value))
                return entry.getKey();
        }
        return null;
    }

    /**
     * Sleep, ignoring interruption. Before using this method, think carefully
     * about why you are ignoring interruptions.
     *
     * @param l
     */
    public static void sleepDeep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    /**
     * If you want to set a transition, please use this method rather than <code>callApiMethod</code> to ensure
     * you really pass an Activity-instance.
     *
     * @param activity the activity-instance for which to set the finish-transition
     * @param enterAnim the incoming-transition of the next activity
     * @param exitAnim the outgoing-transition of this activity
     */
    public static void callOverridePendingTransition(Activity activity, int enterAnim, int exitAnim) {
        callApiMethod(5,
                activity,
                "overridePendingTransition", //$NON-NLS-1$
                new Class<?>[] { Integer.TYPE, Integer.TYPE },
                enterAnim, exitAnim);
    }

    /**
     * Call a method via reflection if API level is at least minSdk
     * @param minSdk minimum sdk number (i.e. 8)
     * @param receiver object to call method on
     * @param methodName method name to call
     * @param params method parameter types
     * @param args arguments
     * @return method return value, or null if nothing was called or exception
     */
    public static Object callApiMethod(int minSdk, Object receiver,
            String methodName, Class<?>[] params, Object... args) {
        if(getSdkVersion() < minSdk)
            return null;

        return AndroidUtilities.callMethod(receiver.getClass(),
                receiver, methodName, params, args);
    }

    /**
     * Call a static method via reflection if API level is at least minSdk
     * @param minSdk minimum sdk number (i.e. 8)
     * @param className fully qualified class to call method on
     * @param methodName method name to call
     * @param params method parameter types
     * @param args arguments
     * @return method return value, or null if nothing was called or exception
     */
    @SuppressWarnings("nls")
    public static Object callApiStaticMethod(int minSdk, String className,
            String methodName, Class<?>[] params, Object... args) {
        if(getSdkVersion() < minSdk)
            return null;

        try {
            return AndroidUtilities.callMethod(Class.forName(className),
                    null, methodName, params, args);
        } catch (ClassNotFoundException e) {
            getExceptionService().reportError("call-method", e);
            return null;
        }
    }

    /**
     * Call a method via reflection
     * @param class class to call method on
     * @param receiver object to call method on (can be null)
     * @param methodName method name to call
     * @param params method parameter types
     * @param args arguments
     * @return method return value, or null if nothing was called or exception
     */
    @SuppressWarnings("nls")
    public static Object callMethod(Class<?> cls, Object receiver,
            String methodName, Class<?>[] params, Object... args) {
        try {
            Method method = cls.getMethod(methodName, params);
            Object result = method.invoke(receiver, args);
            return result;
        } catch (SecurityException e) {
            getExceptionService().reportError("call-method", e);
        } catch (NoSuchMethodException e) {
            getExceptionService().reportError("call-method", e);
        } catch (IllegalArgumentException e) {
            getExceptionService().reportError("call-method", e);
        } catch (IllegalAccessException e) {
            getExceptionService().reportError("call-method", e);
        } catch (InvocationTargetException e) {
            getExceptionService().reportError("call-method", e);
        }

        return null;
    }

    /**
     * From Android MyTracks project (http://mytracks.googlecode.com/).
     * Licensed under the Apache Public License v2
     * @param activity
     * @param id
     * @return
     */
    public static CharSequence readFile(Context activity, int id) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    activity.getResources().openRawResource(id)));
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null) {
                buffer.append(line).append('\n');
            }
            return buffer;
        } catch (IOException e) {
            return ""; //$NON-NLS-1$
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public static String readInputStream(InputStream input) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(input), 1 << 14);
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null) {
                buffer.append(line).append('\n');
            }
            return buffer.toString();
        } catch (IOException e) {
            return ""; //$NON-NLS-1$
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Performs an md5 hash on the input string
     * @param input
     * @return
     */
    @SuppressWarnings("nls")
    public static String md5(String input) {
        try {
            byte[] bytesOfMessage = input.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytesOfMessage);
            BigInteger bigInt = new BigInteger(1,digest);
            String hashtext = bigInt.toString(16);
            while(hashtext.length() < 32 ){
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Create an intent to a remote activity
     * @param appPackage
     * @param activityClass
     * @return
     */
    public static Intent remoteIntent(String appPackage, String activityClass) {
        Intent intent = new Intent();
        intent.setClassName(appPackage, activityClass);
        return intent;
    }

    /**
     * Gets application signature
     * @return application signature, or null if an error was encountered
     */
    public static String getSignature(Context context, String packageName) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES);
            return packageInfo.signatures[0].toCharsString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Join items to a list
     * @param <TYPE>
     * @param list
     * @param newList
     * @param newItems
     * @return
     */
    public static <T> T[] addToArray(Class<T> type, T[] list, T... newItems) {
        int originalListLength = 0;
        int length = 0;
        if (list != null) {
            originalListLength = list.length;
            length += list.length;
        }
        if (newItems != null)
            length += newItems.length;

        T[] newList = (T[]) Array.newInstance(type, length);
        if (list != null) {
            for(int i = 0; i < list.length; i++)
                newList[i] = list[i];
        }
        if (newItems != null) {
            for(int i = 0; i < newItems.length; i++)
                newList[originalListLength + i] = newItems[i];
        }
        return newList;
    }

    // --- internal

    private static ExceptionService exceptionService = null;

    private static ExceptionService getExceptionService() {
        if(exceptionService == null)
            synchronized(AndroidUtilities.class) {
                if(exceptionService == null)
                    exceptionService = new ExceptionService();
            }
        return exceptionService;
    }

    /**
     * Concatenate additional stuff to the end of the array
     * @param params
     * @param additional
     * @return
     */
    public static <TYPE> TYPE[] concat(TYPE[] dest, TYPE[] source, TYPE... additional) {
        int i = 0;
        for(; i < Math.min(dest.length, source.length); i++)
            dest[i] = source[i];
        int base = i;
        for(; i < dest.length; i++)
            dest[i] = additional[i - base];
        return dest;
    }

    /**
     * Returns a map where the keys are the values of the map argument
     * and the values are the corresponding keys. Use at your own
     * risk if your map is not 1-to-1!
     * @param map
     * @return
     */
    public static <K, V> Map<V, K> reverseMap(Map<K, V> map) {
        HashMap<V, K> reversed = new HashMap<V, K>();

        Set<Entry<K, V>> entries = map.entrySet();
        for (Entry<K, V> entry : entries) {
            reversed.put(entry.getValue(), entry.getKey());
        }
        return reversed;
    }

    /**
     * Capitalize the first character
     * @param string
     * @return
     */
    public static String capitalize(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    /**
     * Dismiss the keyboard if it is displayed by any of the listed views
     * @param context
     * @param views - a list of views that might potentially be displaying the keyboard
     */
    public static void hideSoftInputForViews(Context context, View...views) {
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        for (View v : views) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    /**
     * Returns true if the screen is large or xtra large
     * @param context
     * @return
     */
    public static boolean isTabletSized(Context context) {
        if (context.getPackageManager().hasSystemFeature("com.google.android.tv")) //$NON-NLS-1$
            return true;
        int size = context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;

        if (size == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            return true;
        } else if (size == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            float width = metrics.widthPixels / metrics.density;
            float height = metrics.heightPixels / metrics.density;

            float effectiveWidth = Math.min(width, height);
            float effectiveHeight = Math.max(width, height);

            return (effectiveWidth >= MIN_TABLET_WIDTH && effectiveHeight >= MIN_TABLET_HEIGHT);
        } else {
            return false;
        }
    }

    public static final int MIN_TABLET_WIDTH = 550;
    public static final int MIN_TABLET_HEIGHT = 800;

    /**
     * Wraps a call to Activity.unregisterReceiver in a try/catch block to prevent
     * exceptions being thrown if receiver was never registered with that activity
     * @param activity
     * @param receiver
     */
    public static void tryUnregisterReceiver(Activity activity, BroadcastReceiver receiver) {
        try {
            activity.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // Receiver wasn't registered for some reason
        }
    }

    /**
     * Dismiss a popup window (should call from main thread)
     *
     * @param activity
     * @param popup
     */
    public static void tryDismissPopup(Activity activity, final PopupWindow popup) {
        if (popup == null)
            return;
        try {
            popup.dismiss();
        } catch (Exception e) {
            // window already closed or something
        }
    }

    /**
     * Tries to parse an int from a string, returning the default value on failure
     */
    public static int tryParseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Tries to parse an int from a string, returning the default value on failure
     */
    public static long tryParseLong(String str, long defaultValue) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the final word characters after the last '.'
     * @param file
     * @return
     */
    @SuppressWarnings("nls")
    public static String getFileExtension(String file) {
        int index = file.lastIndexOf('.');
        String extension = "";
        if (index > 0) {
            extension = file.substring(index + 1);
            if (!extension.matches("\\w+"))
                extension = "";
        }
        return extension;
    }

    /**
     * Logs a JSONObject using in a readable way
     */
    @SuppressWarnings("nls")
    public static void logJSONObject(String tag, JSONObject object) {
        if (object == null) {
            Log.e(tag, "JSONOBject: null");
            return;
        } else {
            Log.e(tag, "Logging JSONObject");
        }
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONArray array = object.optJSONArray(key);
            if (array != null) {
                Log.e(tag, "    " + key + ": Array");
                for (int i = 0; i < array.length(); i++) {
                    try {
                        Object elem = array.get(i);
                        Log.e(tag, "      Index " + i + ": " + elem);
                    } catch (JSONException e) {/**/}
                }
            } else {
                try {
                    Object value = object.get(key);
                    Log.e(tag, "    " + key + ": " + value);
                } catch (JSONException e) {/**/}
            }
        }

    }

}
