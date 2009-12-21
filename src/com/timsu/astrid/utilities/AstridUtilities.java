package com.timsu.astrid.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.database.sqlite.SQLiteException;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

/**
 * General purpose utilities for the Astrid project. Grab-bag of stuff.
 *
 * @author timsu
 *
 */
public class AstridUtilities {

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
        trace = trace.replaceAll("com.timsu.astrid", "!as");
        trace = trace.replaceAll("com.mdt.rtm", "!rtm");
        trace = trace.replaceAll("android.database.sqlite", "!sqlite");
        trace = trace.replaceAll("android", "!A");
        trace = trace.replaceAll("database", "!db");
        trace = trace.replaceAll("org.apache.harmony.xml.parsers", "!xmlp");
        trace = trace.replaceAll(IllegalStateException.class.getName(), "IlStEx");
        trace = trace.replaceAll(ClassCastException.class.getName(), "ClCaEx");
        trace = trace.replaceAll(NullPointerException.class.getName(), "NPEx");
        trace = trace.replaceAll(SQLiteException.class.getName(), "SqLiEx");
        trace = trace.replaceAll(".java:", ":");

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

