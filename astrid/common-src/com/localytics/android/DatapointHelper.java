//@formatter:off
/**
 * DatapointHelper.java Copyright (C) 2011 Char Software Inc., DBA Localytics This code is provided under the Localytics Modified
 * BSD License. A copy of this license has been distributed in a file called LICENSE with this source code. Please visit
 * www.localytics.com for more information.
 */
//@formatter:on

package com.localytics.android;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides a number of static functions to aid in the collection and formatting of datapoints.
 * <p>
 * Note: this is not a public API.
 */
/* package */final class DatapointHelper
{
    /**
     * AndroidID known to be duplicated across many devices due to manufacturer bugs.
     */
    private static final String INVALID_ANDROID_ID = "9774d56d682e549c"; //$NON-NLS-1$

    /**
     * The path to the device_id file in previous versions of the Localytics library
     */
    private static final String LEGACY_DEVICE_ID_FILE = "/localytics/device_id"; //$NON-NLS-1$

    /**
     * Private constructor prevents instantiation
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private DatapointHelper()
    {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }

    /**
     * @return current Android API level.
     */
    /* package */static int getApiLevel()
    {
        try
        {
            // Although the Build.VERSION.SDK field has existed since API 1, it is deprecated and could be removed
            // in the future. Therefore use reflection to retrieve it for maximum forward compatibility.
            final Class<?> buildClass = Build.VERSION.class;
            final String sdkString = (String) buildClass.getField("SDK").get(null); //$NON-NLS-1$
            return Integer.parseInt(sdkString);
        }
        catch (final Exception e)
        {
            Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$

            // Although probably not necessary, protects from the aforementioned deprecation
            try
            {
                final Class<?> buildClass = Build.VERSION.class;
                return buildClass.getField("SDK_INT").getInt(null); //$NON-NLS-1$
            }
            catch (final Exception ignore)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Caught exception", ignore); //$NON-NLS-1$
                }
            }
        }

        // Worse-case scenario, assume Cupcake
        return 3;
    }

    /**
     * Gets a 1-way hashed value of the device's Android ID. This value is encoded using a SHA-256 one way hash and therefore
     * cannot be used to determine what device this data came from.
     *
     * @param context The context used to access the settings resolver
     * @return An 1-way hashed version of the {@link android.provider.Settings.Secure#ANDROID_ID}. May return null if an Android
     *         ID or the hashing algorithm is not available.
     */
    public static String getAndroidIdHashOrNull(final Context context)
    {
        // Make sure a legacy version of the SDK didn't leave behind a device ID.
        // If it did, this ID must be used to keep user counts accurate
        final File fp = new File(context.getFilesDir() + LEGACY_DEVICE_ID_FILE);
        if (fp.exists() && fp.length() > 0)
        {
            try
            {
                BufferedReader reader = null;
                try
                {
                    final char[] buf = new char[100];
                    int numRead;
                    reader = new BufferedReader(new FileReader(fp), 128);
                    numRead = reader.read(buf);
                    final String deviceId = String.copyValueOf(buf, 0, numRead);
                    reader.close();
                    return deviceId;
                }
                catch (final FileNotFoundException e)
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$
                    }
                }
                finally
                {
                    if (null != reader)
                    {
                        reader.close();
                    }
                }
            }
            catch (final IOException e)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$
                }
            }
        }

        final String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.toLowerCase().equals(INVALID_ANDROID_ID))
        {
            return null;
        }

        return getSha256(androidId);
    }

    /**
     * Gets a 1-way hashed value of the device's unique serial number. This value is encoded using a SHA-256 one way hash and
     * therefore cannot be used to determine what device this data came from.
     * <p>
     * Note: {@link android.os.Build#SERIAL} was introduced in SDK 9. For older SDKs, this method will return null.
     *
     * @return An 1-way hashed version of the {@link android.os.Build#SERIAL}. May return null if a serial or the hashing
     *         algorithm is not available.
     */
    /*
     * Suppress JavaDoc warnings because the {@link android.os.Build#SERIAL} fails when built with SDK 4.
     */
    @SuppressWarnings("javadoc")
    public static String getSerialNumberHashOrNull()
    {
        /*
         * Obtain the device serial number using reflection, since serial number was added in SDK 9
         */
        String serialNumber = null;
        if (Constants.CURRENT_API_LEVEL >= 9)
        {
            try
            {
                serialNumber = (String) Build.class.getField("SERIAL").get(null); //$NON-NLS-1$
            }
            catch (final Exception e)
            {
                /*
                 * This should never happen, as SERIAL is a public field added in SDK 9.
                 */
                throw new RuntimeException(e);
            }
        }

        if (serialNumber == null)
        {
            return null;
        }

        return getSha256(serialNumber);
    }

    /**
     * Gets the device's telephony ID (e.g. IMEI/MEID).
     * <p>
     * Note: this method will return null if {@link permission#READ_PHONE_STATE} is not available. This method will also return
     * null on devices that do not have telephony.
     *
     * @param context The context used to access the phone state.
     * @return An the {@link TelephonyManager#getDeviceId()}. Null if an ID is not available, or if
     *         {@link permission#READ_PHONE_STATE} is not available.
     */
    public static String getTelephonyDeviceIdOrNull(final Context context)
    {
        if (Constants.CURRENT_API_LEVEL >= 8)
        {
            final Boolean hasTelephony = ReflectionUtils.tryInvokeInstance(context.getPackageManager(), "hasSystemFeature", new Class<?>[] { String.class }, new Object[] { "android.hardware.telephony" }); //$NON-NLS-1$//$NON-NLS-2$

            if (!hasTelephony.booleanValue())
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.i(Constants.LOG_TAG, "Device does not have telephony; cannot read telephony id"); //$NON-NLS-1$
                }

                return null;
            }
        }

        /*
         * Note: Sometimes Android will deny a package READ_PHONE_STATE permissions, even if the package has the permission. It
         * appears to be a race condition that occurs during installation.
         */
        String id = null;
        if (context.getPackageManager().checkPermission(permission.READ_PHONE_STATE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED)
        {
            final TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            id = manager.getDeviceId();
        }
        else
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "Application does not have permission READ_PHONE_STATE; determining device id is not possible.  Please consider requesting READ_PHONE_STATE in the AndroidManifest"); //$NON-NLS-1$
            }
        }

        return id;
    }

    /**
     * Gets a 1-way hashed value of the device's IMEI/MEID ID. This value is encoded using a SHA-256 one way hash and cannot be
     * used to determine what device this data came from.
     * <p>
     * Note: this method will return null if this is a non-telephony device.
     * <p>
     * Note: this method will return null if {@link permission#READ_PHONE_STATE} is not available.
     *
     * @param context The context used to access the phone state.
     * @return An 1-way hashed version of the {@link TelephonyManager#getDeviceId()}. Null if an ID or the hashing algorithm is
     *         not available, or if {@link permission#READ_PHONE_STATE} is not available.
     */
    public static String getTelephonyDeviceIdHashOrNull(final Context context)
    {
        final String id = getTelephonyDeviceIdOrNull(context);

        if (null == id)
        {
            return null;
        }

        return getSha256(id);
    }

    /**
     * Determines the type of network this device is connected to.
     *
     * @param context the context used to access the device's WIFI
     * @param telephonyManager The manager used to access telephony info
     * @return The type of network, or unknown if the information is unavailable
     */
    public static String getNetworkType(final Context context, final TelephonyManager telephonyManager)
    {
        if (context.getPackageManager().checkPermission(permission.ACCESS_WIFI_STATE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED)
        {
            final NetworkInfo wifiInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (wifiInfo != null && wifiInfo.isConnectedOrConnecting())
            {
                return "wifi"; //$NON-NLS-1$
            }
        }
        else
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "Application does not have permission ACCESS_WIFI_STATE; determining Wi-Fi connectivity is unavailable"); //$NON-NLS-1$
            }
        }

        return "android_network_type_" + telephonyManager.getNetworkType(); //$NON-NLS-1$
    }

    /**
     * Gets the device manufacturer's name. This is only available on SDK 4 or greater, so on SDK 3 this method returns the
     * constant string "unknown".
     *
     * @return A string naming the manufacturer
     */
    public static String getManufacturer()
    {
        String mfg = "unknown"; //$NON-NLS-1$
        if (Constants.CURRENT_API_LEVEL > 3)
        {
            try
            {
                final Class<?> buildClass = Build.class;
                mfg = (String) buildClass.getField("MANUFACTURER").get(null); //$NON-NLS-1$
            }
            catch (final Exception ignore)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Caught exception", ignore); //$NON-NLS-1$
                }
            }
        }
        return mfg;
    }

    /**
     * Gets the versionName of the application.
     *
     * @param context {@link Context}. Cannot be null.
     * @return The application's version
     */
    public static String getAppVersion(final Context context)
    {
        final PackageManager pm = context.getPackageManager();

        try
        {
            final String versionName = pm.getPackageInfo(context.getPackageName(), 0).versionName;

            /*
             * If there is no versionName in the Android Manifest, the versionName will be null.
             */
            if (versionName == null)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "versionName was null--is a versionName attribute set in the Android Manifest?"); //$NON-NLS-1$
                }

                return "unknown"; //$NON-NLS-1$
            }

            return versionName;
        }
        catch (final PackageManager.NameNotFoundException e)
        {
            /*
             * This should never occur--our own package must exist for this code to be running
             */
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to generate a SHA-256 hash of a given String
     *
     * @param string String to hash. Cannot be null.
     * @return hashed version of the string using SHA-256.
     */
    /* package */static String getSha256(final String string)
    {
        if (Constants.ENABLE_PARAMETER_CHECKING)
        {
            if (null == string)
            {
                throw new IllegalArgumentException("string cannot be null"); //$NON-NLS-1$
            }
        }

        try
        {
            final MessageDigest md = MessageDigest.getInstance("SHA-256"); //$NON-NLS-1$
            final byte[] digest = md.digest(string.getBytes("UTF-8")); //$NON-NLS-1$
            final BigInteger hashedNumber = new BigInteger(1, digest);
            return hashedNumber.toString(16);
        }
        catch (final NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
        catch (final UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
