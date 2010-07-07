/**
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.service;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.database.sqlite.SQLiteException;

import com.flurry.android.FlurryAgent;
import com.todoroo.andlib.service.ExceptionService.ErrorReporter;

public class FlurryReporter implements ErrorReporter {

    @SuppressWarnings("nls")
    public void handleError(String name, Throwable error) {
        if(error == null)
            return;

        String message = error.toString();

        StringWriter writer = new StringWriter();
        PrintWriter writerPrinter = new PrintWriter(writer);
        error.printStackTrace(writerPrinter);
        writerPrinter.flush();
        writerPrinter.close();

        String trace = writer.toString();

        // shorten the string
        trace = trace.substring(message.length());
        trace = trace.replaceAll("com.todoroo.bente", "!ctb");
        trace = trace.replaceAll("com.todoroo.astrid", "!cta");
        trace = trace.replaceAll("com.todoroo.android", "!ctc");
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

}
