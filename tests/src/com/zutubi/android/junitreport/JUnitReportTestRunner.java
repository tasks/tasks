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

import android.os.Bundle;
import android.test.AndroidTestRunner;
import android.test.InstrumentationTestRunner;

/**
 * Custom test runner that adds a {@link JUnitReportListener} to the underlying
 * test runner in order to capture test results in an XML report. You may use
 * this class in place of {@link InstrumentationTestRunner} in your test
 * project's manifest, and/or specify it to your Ant build using the test.runner
 * property.
 * <p/>
 * This runner behaves identically to the default, with the added side-effect of
 * producing a JUnit XML report. The report format is similar to that produced
 * by the Ant JUnit task's XML formatter, making it compatible with existing
 * tools that can process that format. See {@link JUnitReportListener} for
 * further details.
 * <p/>
 * This runner accepts the following arguments:
 * <ul>
 * <li>reportFilePath: path of the file to write the XML report to, in the
 * target application's data area (default: junit-report.xml).</li>
 * <li>filterTraces: if true, stack traces in test failure reports will be
 * filtered to remove noise such as framework methods (default: true)</li>
 * </ul>
 * These arguments may be specified as follows:
 * 
 * <pre>
 * {@code adb shell am instrument -w -e reportFile my-report-file.xml}
 * </pre>
 */
public class JUnitReportTestRunner extends InstrumentationTestRunner {
    /**
     * Path, relative to the target applications file root, at which to write the report file.
     */
    private static final String ARG_REPORT_FILE_PATH = "reportFilePath";
    /**
     * If true, stack traces in the report will be filtered to remove common noise (e.g. framework
     * methods).
     */
    private static final String ARG_FILTER_TRACES = "filterTraces";
    /**
     * Default path of the report file.
     */
    private static final String DEFAULT_REPORT_FILE = "junit-report.xml";

    private JUnitReportListener mListener;
    private String mReportFilePath;
    private boolean mFilterTraces = true;

    @Override
    public void onCreate(Bundle arguments) {
        if (arguments != null) {
            mReportFilePath = arguments.getString(ARG_REPORT_FILE_PATH);
            mFilterTraces = arguments.getBoolean(ARG_FILTER_TRACES, true);
        }

        if (mReportFilePath == null) {
            mReportFilePath = DEFAULT_REPORT_FILE;
        }

        super.onCreate(arguments);
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner() {
        AndroidTestRunner runner = new AndroidTestRunner();
        mListener = new JUnitReportListener(getTargetContext(), mReportFilePath, mFilterTraces);
        runner.addTestListener(mListener);
        return runner;
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        if (mListener != null) {
            mListener.close();
        }

        super.finish(resultCode, results);
    }
}
