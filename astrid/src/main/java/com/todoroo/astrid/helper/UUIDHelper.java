package com.todoroo.astrid.helper;

import java.util.UUID;

public class UUIDHelper {

    private static final long MIN_UUID = 100000000;

    /**
     * @return a pair consisting of the newly
     * generated uuid and the corresponding proof text
     */
    public static String newUUID() {
        long uuid = 0;
        do {
            uuid = UUID.randomUUID().getLeastSignificantBits() & 0x7fffffffffffffffL;;
        } while (uuid < MIN_UUID);
        return Long.toString(uuid);
    }
}
