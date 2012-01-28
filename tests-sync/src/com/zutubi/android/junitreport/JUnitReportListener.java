/*
 * Copyright (C) 2010 Zutubi Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zutubi.android.junitreport;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

/**
 * Custom test listener that outputs test results to a single XML file. The file
 * uses a similar format the to Ant JUnit task XML formatter, with a couple of
 * caveats:
 * <ul>
 * <li>Multiple suites are all placed in a single file under a root
 * &lt;testsuites&gt; element.</li>
 * <li>Redundant information about the number of nested cases within a suite is
 * omitted.</li>
 * <li>Neither standard output nor system properties are included.</li>
 * </ul>
 * The differences mainly revolve around making this reporting as lightweight as
 * possible. The report is streamed as the tests run, making it impossible to,
 * e.g. include the case count in a &lt;testsuite&gt; element.
 */
public class JUnitReportListener implements TestListener {
    private static final String LOG_TAG = "JUnitReportListener";

    private static final String ENCODING_UTF_8 = "utf-8";

    private static final String TAG_SUITES = "testsuites";
    private static final String TAG_SUITE = "testsuite";
    private static final String TAG_CASE = "testcase";
    private static final String TAG_ERROR = "error";
    private static final String TAG_FAILURE = "failure";

    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_CLASS = "classname";
    private static final String ATTRIBUTE_TYPE = "type";
    private static final String ATTRIBUTE_MESSAGE = "message";
    private static final String ATTRIBUTE_TIME = "time";

    // With thanks to org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.
    // Trimmed some entries, added others for Android.
    private static final String[] DEFAULT_TRACE_FILTERS = new String[] {
            "junit.framework.TestCase", "junit.framework.TestResult",
            "junit.framework.TestSuite",
            "junit.framework.Assert.", // don't filter AssertionFailure
            "java.lang.reflect.Method.invoke(", "sun.reflect.",
            // JUnit 4 support:
            "org.junit.", "junit.framework.JUnit4TestAdapter", " more",
            // Added for Android
            "android.test.", "android.app.Instrumentation",
            "java.lang.reflect.Method.invokeNative",
    };

    private Context mContext;
    private String mReportFilePath;
    private boolean mFilterTraces;
    private FileOutputStream mOutputStream;
    private XmlSerializer mSerializer;
    private String mCurrentSuite;

    // simple time tracking
    private boolean timeAlreadyWritten = false;
    private long testStart;

    /**
     * Creates a new listener.
     *
     * @param context context of the target application under test
     * @param reportFilePath path of the report file to create (under the
     *            context using {@link Context#openFileOutput(String, int)}).
     * @param filterTraces if true, stack traces will have common noise (e.g.
     *            framework methods) omitted for clarity
     */
    public JUnitReportListener(Context context, String reportFilePath, boolean filterTraces) {
        this.mContext = context;
        this.mReportFilePath = reportFilePath;
        this.mFilterTraces = filterTraces;
    }

    public void startTest(Test test) {
        try {
            openIfRequired(test);

            if (test instanceof TestCase) {
                TestCase testCase = (TestCase) test;
                checkForNewSuite(testCase);
                testStart = System.currentTimeMillis();
                timeAlreadyWritten = false;
                mSerializer.startTag("", TAG_CASE);
                mSerializer.attribute("", ATTRIBUTE_CLASS, mCurrentSuite);
                mSerializer.attribute("", ATTRIBUTE_NAME, testCase.getName());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, safeMessage(e));
        }
    }

    private void checkForNewSuite(TestCase testCase) throws IOException {
        String suiteName = testCase.getClass().getName();
        if (mCurrentSuite == null || !mCurrentSuite.equals(suiteName)) {
            if (mCurrentSuite != null) {
                mSerializer.endTag("", TAG_SUITE);
            }

            mSerializer.startTag("", TAG_SUITE);
            mSerializer.attribute("", ATTRIBUTE_NAME, suiteName);
            mCurrentSuite = suiteName;
        }
    }

    private void openIfRequired(Test test) throws IOException {
        if (mOutputStream == null) {
            mOutputStream = mContext.openFileOutput(mReportFilePath, 0);
            mSerializer = Xml.newSerializer();
            mSerializer.setOutput(mOutputStream, ENCODING_UTF_8);
            mSerializer.startDocument(ENCODING_UTF_8, true);
            mSerializer.startTag("", TAG_SUITES);
        }
    }

    public void addError(Test test, Throwable error) {
        addProblem(TAG_ERROR, error);
    }

    public void addFailure(Test test, AssertionFailedError error) {
        addProblem(TAG_FAILURE, error);
    }

    private void addProblem(String tag, Throwable error) {
        try {
            recordTestTime();

            mSerializer.startTag("", tag);
            mSerializer.attribute("", ATTRIBUTE_MESSAGE, safeMessage(error));
            mSerializer.attribute("", ATTRIBUTE_TYPE, error.getClass().getName());
            StringWriter w = new StringWriter();
            error.printStackTrace(mFilterTraces ? new FilteringWriter(w) : new PrintWriter(w));
            mSerializer.text(w.toString());
            mSerializer.endTag("", tag);
        } catch (IOException e) {
            Log.e(LOG_TAG, safeMessage(e));
        }
    }

    private void recordTestTime() throws IOException {
        if(!timeAlreadyWritten) {
            timeAlreadyWritten = true;
            mSerializer.attribute("", ATTRIBUTE_TIME,
                    String.format("%.3f", (System.currentTimeMillis() - testStart) / 1000.));
        }
    }

    public void endTest(Test test) {
        try {
            if (test instanceof TestCase) {
                recordTestTime();
                mSerializer.endTag("", TAG_CASE);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, safeMessage(e));
        }
    }

    /**
     * Releases all resources associated with this listener.  Must be called
     * when the listener is finished with.
     */
    public void close() {
        if (mSerializer != null) {
            try {
                if (mCurrentSuite != null) {
                    mSerializer.endTag("", TAG_SUITE);
                }

                mSerializer.endTag("", TAG_SUITES);
                mSerializer.endDocument();
                mSerializer = null;
            } catch (IOException e) {
                Log.e(LOG_TAG, safeMessage(e));
            }
        }

        if (mOutputStream != null) {
            try {
                mOutputStream.close();
                mOutputStream = null;
            } catch (IOException e) {
                Log.e(LOG_TAG, safeMessage(e));
            }
        }
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getName() + ": " + (message == null ? "<null>" : message);
    }

    /**
     * Wrapper around a print writer that filters out common noise from stack
     * traces, making it easier to see the actual failure.
     */
    private static class FilteringWriter extends PrintWriter {
        public FilteringWriter(Writer out) {
            super(out);
        }

        @Override
        public void println(String s) {
            for (String filtered : DEFAULT_TRACE_FILTERS) {
                if (s.contains(filtered)) {
                    return;
                }
            }

            super.println(s);
        }
    }
}
