/*
 * Copyright (C) 2010-2012 Zutubi Pty Ltd
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
import android.util.Log;

/**
 * Custom test runner that adds a {@link JUnitReportListener} to the underlying
 * test runner in order to capture test results in an XML report. You may use
 * this class in place of {@link InstrumentationTestRunner} in your test
 * project's manifest, and/or specify it to your Ant build using the test.runner
 * property.
 * <p/>
 * This runner behaves identically to the default, with the added side-effect of
 * producing JUnit XML reports. The report format is similar to that produced
 * by the Ant JUnit task's XML formatter, making it compatible with existing
 * tools that can process that format. See {@link JUnitReportListener} for
 * further details.
 * <p/>
 * This runner accepts arguments specified by the ARG_* constants.  For details
 * refer to the README.
 */
public class JUnitReportTestRunner extends InstrumentationTestRunner {
    /**
     * Name of the report file(s) to write, may contain __suite__ in multiFile mode.
     */
    private static final String ARG_REPORT_FILE = "reportFile";
    /**
     * If specified, path of the directory to write report files to.  May start with __external__.
     * If not set files are written to the internal storage directory of the app under test.
     */
    private static final String ARG_REPORT_DIR = "reportDir";
    /**
     * If true, stack traces in the report will be filtered to remove common noise (e.g. framework
     * methods).
     */
    private static final String ARG_FILTER_TRACES = "filterTraces";
    /**
     * If true, produce a separate file for each test suite.  By default a single report is created
     * for all suites.
     */
    private static final String ARG_MULTI_FILE = "multiFile";
    /**
     * Default name of the single report file.
     */
    private static final String DEFAULT_SINGLE_REPORT_FILE = "junit-report.xml";
    /**
     * Default name pattern for multiple report files.
     */
    private static final String DEFAULT_MULTI_REPORT_FILE = "junit-report-" + JUnitReportListener.TOKEN_SUITE + ".xml";

    private static final String LOG_TAG = JUnitReportTestRunner.class.getSimpleName();
    
    private JUnitReportListener mListener;
    private String mReportFile;
    private String mReportDir;
    private boolean mFilterTraces = true;
    private boolean mMultiFile = false;

    @Override
    public void onCreate(Bundle arguments) {
        if (arguments != null) {
            Log.i(LOG_TAG, "Created with arguments: " + arguments.keySet());
            mReportFile = arguments.getString(ARG_REPORT_FILE);
            mReportDir = arguments.getString(ARG_REPORT_DIR);
            mFilterTraces = getBooleanArgument(arguments, ARG_FILTER_TRACES, true);
            mMultiFile = getBooleanArgument(arguments, ARG_MULTI_FILE, false);
        } else {
            Log.i(LOG_TAG, "No arguments provided");
        }

        if (mReportFile == null) {
            mReportFile = mMultiFile ? DEFAULT_MULTI_REPORT_FILE : DEFAULT_SINGLE_REPORT_FILE;
            Log.i(LOG_TAG, "Defaulted report file to '" + mReportFile + "'");
        }

        super.onCreate(arguments);
    }

    private boolean getBooleanArgument(Bundle arguments, String name, boolean defaultValue)
    {
        String value = arguments.getString(name);
        if (value == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(value);
        }
    }

    /**
     * Subclass and override this if you want to use a different TestRunner type.
     * 
     * @return the test runner to use
     */
    protected AndroidTestRunner makeAndroidTestRunner() {
        return new AndroidTestRunner();
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner() {
        AndroidTestRunner runner = makeAndroidTestRunner();
        mListener = new JUnitReportListener(getContext(), getTargetContext(), mReportFile, mReportDir, mFilterTraces, mMultiFile);
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
