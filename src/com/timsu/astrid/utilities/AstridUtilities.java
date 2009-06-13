package com.timsu.astrid.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.database.sqlite.SQLiteException;

import com.flurry.android.FlurryAgent;

/**
 * General purpose utilities for the Astrid project. Grab-bag of stuff.
 *
 * @author timsu
 *
 */
public class AstridUtilities {

    /**
     * Converts a throwable's stack trace into a string
     *
     * @param input
     * @return
     */
    public static void reportFlurryError(String name, Throwable error) {
        String message = error.toString();

        StringWriter writer = new StringWriter();
        PrintWriter writerPrinter = new PrintWriter(writer);
        error.printStackTrace(writerPrinter);
        writerPrinter.flush();
        writerPrinter.close();

        String trace = writer.toString();

        // shorten the string
        trace = trace.substring(message.length());
        trace.replaceAll("android", "A");
        trace.replaceAll("database", "db");
        trace.replaceAll(IllegalStateException.class.getName(), "IlStEx");
        trace.replaceAll(ClassCastException.class.getName(), "ClCaEx");
        trace.replaceAll(NullPointerException.class.getName(), "NPE");
        trace.replaceAll(SQLiteException.class.getName(), "SqLiEx");
        trace.replaceAll("com.timsu.", "");

        FlurryAgent.onError(name, message, trace);
    }


    /**
     * For reporting uncaught exceptions
     *
     * @author timsu
     *
     */
    public static class AstridUncaughtExceptionHandler implements UncaughtExceptionHandler {
        private UncaughtExceptionHandler defaultUEH;

        public AstridUncaughtExceptionHandler() {
            defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            AstridUtilities.reportFlurryError("uncaught", ex);
            defaultUEH.uncaughtException(thread, ex);
        }
    }
}

