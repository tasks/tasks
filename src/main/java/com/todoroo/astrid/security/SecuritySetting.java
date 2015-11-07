package com.todoroo.astrid.security;

/**
 * Created by Tio on 11/7/15.
 * This class handle users' or default settings of encryption.
 */

public class SecuritySetting {

    private static String str;

    public static boolean securityMode = true;

    public void createSecuritySetting() {

        // Create txt for saving

    }

    public void createDefaultSetting() {

        // Write default setting to txt

    }

    public String createKey() {

        // Random
        return null;

    }

    public void updateKey() {

        // Change key

    }

    public String getKey() {

        // Get current key
        return null;

    }

    public void securityModeOn () {

        securityMode = true;

    }

    public void securityModeOff () {

        securityMode = false;

    }

}