package org.tasks.time;

import java.util.Date;

public class DateTimeUtils {

    private static final SystemMillisProvider SYSTEM_MILLIS_PROVIDER = new SystemMillisProvider();
    private static volatile MillisProvider MILLIS_PROVIDER = SYSTEM_MILLIS_PROVIDER;

    public static long currentTimeMillis() {
        return MILLIS_PROVIDER.getMillis();
    }

    public static void setCurrentMillisFixed(long millis) {
        MILLIS_PROVIDER = new FixedMillisProvider(millis);
    }

    public static void setCurrentMillisSystem() {
        MILLIS_PROVIDER = SYSTEM_MILLIS_PROVIDER;
    }

    public static String printTimestamp(long timestamp) {
        return new Date(timestamp).toString();
    }
}
