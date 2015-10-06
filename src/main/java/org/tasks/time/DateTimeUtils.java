package org.tasks.time;

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
}
