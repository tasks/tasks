package com.timsu.astrid.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;

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
    public static String throwableToString(Throwable input) {
        StringWriter writer = new StringWriter();
        PrintWriter writerPrinter = new PrintWriter(writer);
        input.printStackTrace(writerPrinter);
        writerPrinter.flush();
        writerPrinter.close();

        return writer.toString();
    }

}
