package com.todoroo.astrid.helper;

import java.security.SecureRandom;
import java.util.UUID;

import android.util.Base64;

import com.todoroo.andlib.utility.Pair;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;

public class UUIDHelper {

    private static final String PREF_DEVICE_ID = "pref_device_id"; //$NON-NLS-1$

    private static final String PREF_LAST_KNOWN_USER = "pref_last_known_user"; //$NON-NLS-1$

    public static String getDeviceId() {
        long lastUser = Preferences.getLong(PREF_LAST_KNOWN_USER, 0);
        long currentUser = ActFmPreferenceService.userId();
        if (currentUser > 0 && lastUser > 0 && currentUser != lastUser) { // User has changed
            Preferences.clear(PREF_DEVICE_ID);
            Preferences.setLong(PREF_LAST_KNOWN_USER, currentUser);
        } else if (currentUser > 0 && lastUser <= 0) {
            Preferences.setLong(PREF_LAST_KNOWN_USER, currentUser);
        }

        String saved = Preferences.getStringValue(PREF_DEVICE_ID);
        if (saved != null)
            return saved;

        saved = UUID.randomUUID().toString();
        Preferences.setString(PREF_DEVICE_ID, saved);
        return saved;
    }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long MIN_UUID = 100000000;

    /**
     * @return a pair consisting of the newly
     * generated uuid and the corresponding proof text
     */
    public static Pair<Long, String> newUUID() {
        long uuid = 0;
        String proofText = ""; //$NON-NLS-1$
        do {
            byte[] chars = new byte[40];
            RANDOM.nextBytes(chars);
            String s = new String(chars);
            String d = getDeviceId();

            proofText = d + "," + s; //$NON-NLS-1$
            uuid = bcryptToLong(proofText);
        } while (uuid >= 0 && uuid < MIN_UUID);
        return Pair.create(uuid, proofText);
    }

    private static final String SALT = "$2a$10$2RHhxhKaPb4VXlQUJyBU/O"; //$NON-NLS-1$

    private static long bcryptToLong(String proofText) {
        String a = BCrypt.hashpw(proofText, SALT);
        int start = 29;
        int endPlusOne = 60;
        String b = a.substring(start, endPlusOne);
        byte[] bytes = Base64.decode(b, Base64.DEFAULT);
        long result = 1;
        for (byte element : bytes) {
            result = 31 * result + element;
        }
        return result;
    }

}
